package com.pasteleria.pos.service;

import com.pasteleria.pos.config.AppConstants;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.SaleItem;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.DiscountType;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.dto.CreateSaleRequest;
import com.pasteleria.pos.dto.SaleItemRequest;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import com.pasteleria.pos.util.DiscountCalculator;
import com.pasteleria.pos.util.MixedPaymentCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final ShiftService shiftService;
    private final CashRegisterService cashRegisterService;
    private final UserService userService;

    public SaleService(
            SaleRepository saleRepository,
            ProductService productService,
            ShiftService shiftService,
            CashRegisterService cashRegisterService,
            UserService userService) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.shiftService = shiftService;
        this.cashRegisterService = cashRegisterService;
        this.userService = userService;
    }

    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {
        validatePayment(request);
        DiscountType totalDiscountType = normalizeDiscountType(request.totalDiscountType(), request.totalDiscountValue());
        BigDecimal totalDiscountValue = normalizeDiscountValue(request.totalDiscountType(), request.totalDiscountValue());
        DiscountCalculator.validateDiscount(totalDiscountType, totalDiscountValue);

        UserPrincipal principal = SecurityUtils.currentUser();
        User seller = userService.getUserEntity(principal.getId());

        cashRegisterService.getOpenCashRegisterForToday();
        Shift shift = shiftService.getRequiredActiveShiftForSeller(principal.getId());

        Sale sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setShift(shift);
        sale.setSeller(seller);
        sale.setPaymentMethod(request.paymentMethod());
        sale.setInstallments(request.paymentMethod() == PaymentMethod.TARJETA ? request.installments() : null);
        sale.setTotalDiscountType(totalDiscountType);
        sale.setTotalDiscountValue(totalDiscountValue);
        sale.setBirthday(false);

        List<SaleItem> items = buildItems(sale, request.items());
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

        sale.setItems(items);
        sale.setSubtotal(totals.subtotal());
        sale.setDiscountTotal(totals.discountTotal());
        sale.setTotal(totals.total());
        sale.setCashAmount(MixedPaymentCalculator.resolveCashAmount(
                request.paymentMethod(), cashPortion, totals.total()));

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
        sale.setPaymentMethod(PaymentMethod.EFECTIVO);
        sale.setBirthday(true);
        sale.setSubtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        sale.setDiscountTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        sale.setTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        sale.setCashAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

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

    public SaleResponse getSale(UUID id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        return DtoMapper.toSaleResponse(sale);
    }

    private List<SaleItem> buildItems(Sale sale, List<SaleItemRequest> itemRequests) {
        List<SaleItem> items = new ArrayList<>();

        for (SaleItemRequest itemRequest : itemRequests) {
            DiscountType itemDiscountType = normalizeDiscountType(itemRequest.discountType(), itemRequest.discountValue());
            BigDecimal itemDiscountValue = normalizeDiscountValue(itemRequest.discountType(), itemRequest.discountValue());
            DiscountCalculator.validateDiscount(itemDiscountType, itemDiscountValue);
            Product product = productService.getActiveProduct(itemRequest.productId());
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
}
