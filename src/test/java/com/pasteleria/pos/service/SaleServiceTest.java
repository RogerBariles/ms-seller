package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.domain.enums.SaleStatus;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.CreateSaleRequest;
import com.pasteleria.pos.dto.SaleItemRequest;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SaleServiceTest {

    private SaleRepository saleRepository;
    private ProductService productService;
    private ShiftService shiftService;
    private CashRegisterService cashRegisterService;
    private UserService userService;
    private StockService stockService;
    private ShiftCashMovementService cashMovementService;
    private SaleService saleService;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID SELLER_COMPANY_ID = UUID.randomUUID();
    private User seller;
    private MockedStatic<SecurityUtils> securityUtilsMock;
    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        productService = mock(ProductService.class);
        shiftService = mock(ShiftService.class);
        cashRegisterService = mock(CashRegisterService.class);
        userService = mock(UserService.class);
        stockService = mock(StockService.class);
        cashMovementService = mock(ShiftCashMovementService.class);
        saleService = new SaleService(
                saleRepository, productService, shiftService, cashRegisterService, userService, stockService,
                cashMovementService);

        seller = createSeller();
        UserPrincipal principal = new UserPrincipal(seller);
        when(userService.getUserEntity(SELLER_ID)).thenReturn(seller);

        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(principal);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);

        securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(ctx);

        when(cashRegisterService.getOpenCashRegisterForToday()).thenReturn(null);
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        when(shiftService.getRequiredShiftForSale(any())).thenReturn(shift);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }

    @Test
    void buildItemsWithMatchingCompanySucceeds() {
        UUID productId = UUID.randomUUID();
        Product product = createProductWithCompany(productId, SELLER_COMPANY_ID, "Product A", true);

        when(productService.getActiveProduct(productId)).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSaleRequest request = new CreateSaleRequest(
                List.of(new SaleItemRequest(productId, 1, null, null)),
                PaymentMethod.EFECTIVO,
                null, null, null, null, null);

        assertDoesNotThrow(() -> saleService.createSale(request));
    }

    @Test
    void buildItemsWithMismatchedCompanyThrows() {
        UUID productId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        Product product = createProductWithCompany(productId, otherCompanyId, "Other Product", true);

        when(productService.getActiveProduct(productId)).thenReturn(product);

        CreateSaleRequest request = new CreateSaleRequest(
                List.of(new SaleItemRequest(productId, 1, null, null)),
                PaymentMethod.EFECTIVO,
                null, null, null, null, null);

        assertThrows(ApiException.class, () -> saleService.createSale(request));
    }

    @Test
    void buildItemsWithNullCompanyProductSkipsValidation() {
        UUID productId = UUID.randomUUID();
        Product product = createProductWithCompany(productId, null, "Birthday Product", true);

        when(productService.getActiveProduct(productId)).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSaleRequest request = new CreateSaleRequest(
                List.of(new SaleItemRequest(productId, 1, null, null)),
                PaymentMethod.EFECTIVO,
                null, null, null, null, null);

        assertDoesNotThrow(() -> saleService.createSale(request));
    }

    @Test
    void cancelCashSaleRefundsOnItsOpenShiftAndRestoresStock() {
        Shift openShift = shiftWithStatus(ShiftStatus.OPEN);
        Sale sale = saleWith(PaymentMethod.EFECTIVO, new BigDecimal("1500.00"), openShift);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.cancelSale(sale.getId(), "  Cliente devolvió la torta  ");

        verify(cashMovementService).registerSaleRefund(
                eq(openShift), eq(new BigDecimal("1500.00")), contains("Cliente devolvió la torta"), eq(seller));
        verify(stockService).restoreOnSaleCancellation(eq(sale), eq(seller), contains("Cliente devolvió la torta"));
        assertEquals(SaleStatus.CANCELLED, response.status());
        assertEquals("Cliente devolvió la torta", response.detail());
        assertNotNull(response.cancelledAt());
        assertEquals("Seller", response.cancelledByName());
    }

    @Test
    void cancelCardSaleDoesNotTouchCash() {
        Sale sale = saleWith(PaymentMethod.TARJETA, BigDecimal.ZERO, shiftWithStatus(ShiftStatus.CLOSED));
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.cancelSale(sale.getId(), "Error de carga");

        verify(cashMovementService, never()).registerSaleRefund(any(), any(), any(), any());
        verify(stockService).restoreOnSaleCancellation(eq(sale), eq(seller), any());
        assertEquals(SaleStatus.CANCELLED, response.status());
    }

    @Test
    void cancelCashSaleOfClosedShiftUsesAnotherOpenShift() {
        Sale sale = saleWith(PaymentMethod.EFECTIVO, new BigDecimal("800.00"), shiftWithStatus(ShiftStatus.CLOSED));
        Shift currentShift = shiftWithStatus(ShiftStatus.OPEN);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shiftService.findOpenShiftForCompany(SELLER_ID, SELLER_COMPANY_ID)).thenReturn(Optional.of(currentShift));

        saleService.cancelSale(sale.getId(), "Se anuló al día siguiente");

        verify(cashMovementService).registerSaleRefund(
                eq(currentShift), eq(new BigDecimal("800.00")), any(), eq(seller));
    }

    @Test
    void cancelCashSaleWithoutOpenShiftFailsBeforeTouchingStock() {
        Sale sale = saleWith(PaymentMethod.EFECTIVO, new BigDecimal("800.00"), shiftWithStatus(ShiftStatus.CLOSED));
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(shiftService.findOpenShiftForCompany(SELLER_ID, SELLER_COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> saleService.cancelSale(sale.getId(), "Motivo"));
        verify(stockService, never()).restoreOnSaleCancellation(any(), any(), any());
        assertEquals(SaleStatus.SOLD, sale.getStatus());
    }

    @Test
    void cancelAlreadyCancelledSaleThrows() {
        Sale sale = saleWith(PaymentMethod.EFECTIVO, new BigDecimal("100.00"), shiftWithStatus(ShiftStatus.OPEN));
        sale.setStatus(SaleStatus.CANCELLED);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

        assertThrows(ApiException.class, () -> saleService.cancelSale(sale.getId(), "Otra vez"));
        verify(cashMovementService, never()).registerSaleRefund(any(), any(), any(), any());
    }

    @Test
    void cancelWithBlankDetailThrows() {
        Sale sale = saleWith(PaymentMethod.EFECTIVO, new BigDecimal("100.00"), shiftWithStatus(ShiftStatus.OPEN));
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

        assertThrows(ApiException.class, () -> saleService.cancelSale(sale.getId(), "   "));
    }

    private static Shift shiftWithStatus(ShiftStatus status) {
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setStatus(status);
        return shift;
    }

    private Sale saleWith(PaymentMethod method, BigDecimal cashAmount, Shift shift) {
        Sale sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setSeller(seller);
        sale.setCompany(seller.getCompany());
        sale.setShift(shift);
        sale.setPaymentMethod(method);
        sale.setSubtotal(new BigDecimal("1500.00"));
        sale.setDiscountTotal(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal("1500.00"));
        sale.setCashAmount(cashAmount);
        return sale;
    }

    private static Product createProductWithCompany(UUID productId, UUID companyId, String name, boolean active) {
        Product product = new Product();
        product.setId(productId);
        product.setName(name);
        product.setCategory(ProductCategory.TORTAS);
        product.setPrice(new BigDecimal("100.00"));
        product.setPurchasePrice(new BigDecimal("50.00"));
        product.setActive(active);
        if (companyId != null) {
            Company company = new Company();
            company.setId(companyId);
            company.setName("Company");
            company.setActive(true);
            product.setCompany(company);
        }
        return product;
    }

    private static User createSeller() {
        User seller = new User();
        seller.setId(SELLER_ID);
        seller.setName("Seller");
        seller.setUsername("seller");
        seller.setRole(UserRole.SELLER);
        seller.setActive(true);
        if (SELLER_COMPANY_ID != null) {
            Company company = new Company();
            company.setId(SELLER_COMPANY_ID);
            company.setName("Seller Company");
            company.setActive(true);
            seller.setCompany(company);
        }
        return seller;
    }
}
