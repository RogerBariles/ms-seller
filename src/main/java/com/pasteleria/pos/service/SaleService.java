package com.pasteleria.pos.service;

import com.pasteleria.pos.config.AppConstants;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.SaleItem;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.DiscountType;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.SaleStatus;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.CreateSaleRequest;
import com.pasteleria.pos.dto.SaleItemRequest;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import com.pasteleria.pos.util.AccreditationDateCalculator;
import com.pasteleria.pos.util.DiscountCalculator;
import com.pasteleria.pos.util.MixedPaymentCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final ShiftService shiftService;
    private final CashRegisterService cashRegisterService;
    private static final Logger log = LoggerFactory.getLogger(SaleService.class);
    /** Descuento de bienvenida (%) en la primera compra de un cliente, sobre el total final. */
    static final BigDecimal WELCOME_DISCOUNT_PERCENT = BigDecimal.valueOf(5);
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter SALE_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final UserService userService;
    private final StockService stockService;
    private final ShiftCashMovementService cashMovementService;
    private final CustomerService customerService;

    public SaleService(
            SaleRepository saleRepository,
            ProductService productService,
            ShiftService shiftService,
            CashRegisterService cashRegisterService,
            UserService userService,
            StockService stockService,
            ShiftCashMovementService cashMovementService,
            CustomerService customerService) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.shiftService = shiftService;
        this.cashRegisterService = cashRegisterService;
        this.userService = userService;
        this.stockService = stockService;
        this.cashMovementService = cashMovementService;
        this.customerService = customerService;
    }

    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {
        validatePayment(request);
        DiscountType totalDiscountType = normalizeDiscountType(request.totalDiscountType(), request.totalDiscountValue());
        BigDecimal totalDiscountValue = normalizeDiscountValue(request.totalDiscountType(), request.totalDiscountValue());
        DiscountCalculator.validateDiscount(totalDiscountType, totalDiscountValue);

        UserPrincipal principal = SecurityUtils.currentUser();
        User seller = userService.getUserEntity(principal.getId());

        // SUPER_ADMIN no necesita caja abierta hoy: vende sobre el turno abierto que haya
        if (principal.getRole() != UserRole.SUPER_ADMIN) {
            cashRegisterService.getOpenCashRegisterForToday();
        }
        Shift shift = shiftService.getRequiredShiftForSale(principal);

        Sale sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setShift(shift);
        sale.setSeller(seller);
        sale.setCompany(seller.getCompany());
        sale.setPaymentMethod(request.paymentMethod());
        sale.setInstallments(request.paymentMethod() == PaymentMethod.TARJETA ? request.installments() : null);
        sale.setTotalDiscountType(totalDiscountType);
        sale.setTotalDiscountValue(totalDiscountValue);
        sale.setBirthday(false);
        boolean welcomeDiscount = false;
        if (request.customerId() != null) {
            sale.setCustomer(customerService.getEntity(request.customerId()));
            welcomeDiscount = customerService.isFirstPurchase(request.customerId());
        }

        List<SaleItem> items = buildItems(sale, seller, request.items());

        // Deduct stock for each item sold
        for (SaleItem item : items) {
            stockService.deductOnSale(item.getProduct().getId(), item.getQuantity(), sale.getId());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal lineDiscountTotal = BigDecimal.ZERO;
        for (SaleItem item : items) {
            subtotal = subtotal.add(item.getLineSubtotal());
            lineDiscountTotal = lineDiscountTotal.add(item.getLineDiscount());
        }
        BigDecimal afterLineDiscounts = MixedPaymentCalculator.afterLineDiscounts(subtotal, lineDiscountTotal);
        BigDecimal cashPortion = MixedPaymentCalculator.resolvePartialCash(
                request.paymentMethod(), request.cashAmount(), afterLineDiscounts);
        MixedPaymentCalculator.Totals totals = MixedPaymentCalculator.calculateTotals(
                subtotal,
                lineDiscountTotal,
                totalDiscountType,
                totalDiscountValue,
                request.manualTotal(),
                cashPortion);

        if (totals.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El total de la venta debe ser mayor a cero");
        }

        // 5% de bienvenida sobre el total final (después de descuentos, recargos o total manual).
        // Con pago mixto se descuenta de la parte no efectivo; el efectivo nunca supera el total.
        BigDecimal customerDiscount = welcomeDiscount
                ? totals.total().multiply(WELCOME_DISCOUNT_PERCENT)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalTotal = totals.total().subtract(customerDiscount);
        BigDecimal finalCashPortion = cashPortion != null ? cashPortion.min(finalTotal) : null;

        sale.setItems(items);
        sale.setSubtotal(totals.subtotal());
        sale.setDiscountTotal(totals.discountTotal().add(customerDiscount));
        sale.setCustomerDiscount(customerDiscount);
        sale.setTotal(finalTotal);
        sale.setCashAmount(MixedPaymentCalculator.resolveCashAmount(
                request.paymentMethod(), finalCashPortion, finalTotal));
        sale.setAccreditedAt(AccreditationDateCalculator.calculate(
                sale.getPaymentMethod(), sale.getCreatedAt()));

        return DtoMapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse createBirthdaySale(User celebrant, Shift shift) {
        Product birthdayProduct = productService.getProductEntity(AppConstants.BIRTHDAY_PRODUCT_ID);
        String itemName = "Feliz cumpleaños " + celebrant.getName();

        Sale sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setShift(shift);
        sale.setSeller(celebrant);
        sale.setCompany(celebrant.getCompany());
        sale.setPaymentMethod(PaymentMethod.EFECTIVO);
        sale.setBirthday(true);
        sale.setSubtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        sale.setDiscountTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        sale.setTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        sale.setCashAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        sale.setAccreditedAt(AccreditationDateCalculator.calculate(
                sale.getPaymentMethod(), sale.getCreatedAt()));

        SaleItem item = new SaleItem();
        item.setId(UUID.randomUUID());
        item.setSale(sale);
        item.setProduct(birthdayProduct);
        item.setProductName(itemName);
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setUnitPurchasePrice(birthdayProduct.getPurchasePrice());
        item.setLineSubtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setLineDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setLineTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        sale.setItems(List.of(item));
        return DtoMapper.toSaleResponse(saleRepository.save(sale));
    }

    /**
     * Anula una venta: repone el stock que descontó y, si se cobró algo en efectivo,
     * lo devuelve con un retiro en un turno abierto (el de la venta si sigue abierto).
     */
    @Transactional
    public SaleResponse cancelSale(UUID id, String detail) {
        log.info("Anulación de venta {}: inicio", id);
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        if (sale.isCancelled()) {
            throw new ApiException(HttpStatus.CONFLICT, "La venta ya está anulada");
        }
        if (sale.isBirthday()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las ventas de cumpleaños no se pueden anular");
        }
        String reason = detail == null ? "" : detail.trim();
        if (reason.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Indicá el motivo de la anulación");
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        OffsetDateTime now = OffsetDateTime.now(ZONE);

        BigDecimal cashToRefund = sale.getCashAmount() != null ? sale.getCashAmount() : BigDecimal.ZERO;
        log.info("Anulación de venta {}: usuario={} rol={} empresaUsuario={} empresaVenta={} creada={} pago={} total={} efectivo={} turno={} estadoTurno={}",
                id, principal.getId(), principal.getRole(), principal.getCompanyId(),
                sale.getCompany() != null ? sale.getCompany().getId() : null,
                sale.getCreatedAt(), sale.getPaymentMethod(), sale.getTotal(), cashToRefund,
                sale.getShift() != null ? sale.getShift().getId() : null,
                sale.getShift() != null ? sale.getShift().getStatus() : null);
        if (cashToRefund.compareTo(BigDecimal.ZERO) > 0) {
            Shift refundShift = resolveRefundShift(sale, principal);
            log.info("Anulación de venta {}: devolución de {} en el turno {}", id, cashToRefund, refundShift.getId());
            cashMovementService.registerSaleRefund(refundShift, cashToRefund, refundDetail(sale, reason), user);
            log.info("Anulación de venta {}: retiro registrado", id);
        }

        stockService.restoreOnSaleCancellation(sale, user, "Anulación de venta: " + reason);
        log.info("Anulación de venta {}: stock repuesto", id);

        sale.setStatus(SaleStatus.CANCELLED);
        sale.setDetail(reason);
        sale.setCancelledAt(now);
        sale.setCancelledBy(user);
        SaleResponse response = DtoMapper.toSaleResponse(saleRepository.saveAndFlush(sale));
        log.info("Anulación de venta {}: completada", id);
        return response;
    }

    /** El efectivo sale del turno de la venta si sigue abierto; si no, de un turno abierto de la empresa. */
    private Shift resolveRefundShift(Sale sale, UserPrincipal principal) {
        Shift saleShift = sale.getShift();
        if (saleShift != null && saleShift.getStatus() == ShiftStatus.OPEN) {
            return saleShift;
        }
        UUID companyId = sale.getCompany() != null
                ? sale.getCompany().getId()
                : sale.getSeller().getCompany().getId();
        return shiftService.findOpenShiftForCompany(principal.getId(), companyId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "La venta tiene efectivo para devolver y no hay ningún turno abierto. "
                                + "Abrí la caja y un turno para registrar la devolución."));
    }

    private static String refundDetail(Sale sale, String reason) {
        String text = "Anulación venta del "
                + sale.getCreatedAt().atZoneSameInstant(ZONE).format(SALE_DATE)
                + ": " + reason;
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(UUID id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        return DtoMapper.toSaleResponse(sale);
    }

    private List<SaleItem> buildItems(Sale sale, User seller, List<SaleItemRequest> itemRequests) {
        List<SaleItem> items = new ArrayList<>();

        for (SaleItemRequest itemRequest : itemRequests) {
            DiscountType itemDiscountType = normalizeDiscountType(itemRequest.discountType(), itemRequest.discountValue());
            BigDecimal itemDiscountValue = normalizeDiscountValue(itemRequest.discountType(), itemRequest.discountValue());
            DiscountCalculator.validateDiscount(itemDiscountType, itemDiscountValue);
            Product product = productService.getActiveProduct(itemRequest.productId());

            // Company validation: product.company must match seller.company (skip when null)
            validateProductCompany(product, seller);

            BigDecimal lineSubtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineDiscount = DiscountCalculator.applyDiscount(
                    lineSubtotal, itemDiscountType, itemDiscountValue);
            BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP);

            SaleItem item = new SaleItem();
            item.setId(UUID.randomUUID());
            item.setSale(sale);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getPrice());
            item.setUnitPurchasePrice(product.getPurchasePrice());
            item.setDiscountType(itemDiscountType);
            item.setDiscountValue(itemDiscountValue);
            item.setLineSubtotal(lineSubtotal);
            item.setLineDiscount(lineDiscount);
            item.setLineTotal(lineTotal);
            items.add(item);
        }
        return items;
    }

    private void validatePayment(CreateSaleRequest request) {
        if (request.paymentMethod() == PaymentMethod.TARJETA) {
            if (request.installments() == null || request.installments() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar cantidad de cuotas para tarjeta");
            }
        }
    }

    private static DiscountType normalizeDiscountType(DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return type;
    }

    private static BigDecimal normalizeDiscountValue(DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value;
    }

    private void validateProductCompany(Product product, User seller) {
        if (product.getCompany() == null) {
            return; // Skip validation for null-company products (e.g. birthday product)
        }
        Company sellerCompany = seller.getCompany();
        if (sellerCompany == null || !product.getCompany().getId().equals(sellerCompany.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El producto " + product.getName() + " no pertenece a la empresa de la vendedora");
        }
    }
}
