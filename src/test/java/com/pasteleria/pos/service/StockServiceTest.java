package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.StockMovement;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.StockMovementType;
import com.pasteleria.pos.domain.enums.StockReferenceType;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.StockAdjustRequest;
import com.pasteleria.pos.dto.StockPurchaseRequest;
import com.pasteleria.pos.dto.StockResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.ProductRepository;
import com.pasteleria.pos.repository.StockMovementRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StockServiceTest {

    private StockMovementRepository stockMovementRepository;
    private ProductRepository productRepository;
    private UserService userService;
    private StockService stockService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    private Company company;
    private Product product;
    private User user;
    private UserPrincipal principal;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stockMovementRepository = mock(StockMovementRepository.class);
        productRepository = mock(ProductRepository.class);
        userService = mock(UserService.class);

        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        company.setActive(true);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setUsername("testuser");
        user.setRole(UserRole.ADMIN);
        user.setCompany(company);
        user.setActive(true);

        user.setPasswordHash("pwd");
        principal = new UserPrincipal(user);

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setCurrentStock(50);
        product.setCompany(company);

        stockService = new StockService(stockMovementRepository, productRepository, userService);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    private void mockSecurity() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(principal);
        when(userService.getUserEntity(principal.getId())).thenReturn(user);
    }

    @Test
    void getCurrentStockReturnsProductStock() {
        mockSecurity();
        when(productRepository.findByIdAndCompanyId(product.getId(), company.getId())).thenReturn(Optional.of(product));

        StockResponse response = stockService.getCurrentStock(product.getId());

        assertNotNull(response);
        assertEquals(product.getId(), response.productId());
        assertEquals(product.getName(), response.productName());
        assertEquals(50, response.currentStock());
    }

    @Test
    void getCurrentStockThrowsForNonExistentProduct() {
        mockSecurity();
        UUID missingId = UUID.randomUUID();
        when(productRepository.findByIdAndCompanyId(missingId, company.getId())).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> stockService.getCurrentStock(missingId));
    }

    @Test
    void getCurrentStockThrowsForOtherCompany() {
        mockSecurity();
        Company otherCompany = new Company();
        otherCompany.setId(UUID.randomUUID());
        otherCompany.setName("Other Company");

        product.setCompany(otherCompany);

        when(productRepository.findByIdAndCompanyId(product.getId(), company.getId())).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> stockService.getCurrentStock(product.getId()));
    }

    @Test
    void newProductDefaultsToZeroStock() {
        mockSecurity();
        Product newProduct = new Product();
        newProduct.setId(UUID.randomUUID());
        newProduct.setName("New Product");
        newProduct.setCompany(company);
        // currentStock intentionally left null — defaults to 0 via DB

        when(productRepository.findByIdAndCompanyId(newProduct.getId(), company.getId()))
                .thenReturn(Optional.of(newProduct));

        StockResponse response = stockService.getCurrentStock(newProduct.getId());

        assertNotNull(response);
        assertEquals(0, response.currentStock());
    }

    @Test
    void deductOnSaleReducesStockAndRecordsMovement() {
        mockSecurity();
        UUID saleId = UUID.randomUUID();

        when(productRepository.findByIdAndCompanyId(product.getId(), company.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockService.deductOnSale(product.getId(), 3, saleId);

        assertEquals(47, product.getCurrentStock()); // 50 - 3
        verify(stockMovementRepository).save(any(StockMovement.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void adjustStockUpdatesAndReturnsNewStock() {
        mockSecurity();
        StockAdjustRequest request = new StockAdjustRequest(product.getId(), -10, "Damaged goods");

        when(productRepository.findByIdAndCompanyId(product.getId(), company.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = stockService.adjustStock(request);

        assertNotNull(response);
        assertEquals(product.getId(), response.productId());
        assertEquals(40, response.currentStock());
        verify(stockMovementRepository).save(any(StockMovement.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void adjustStockAllowsNegativeStock() {
        mockSecurity();
        product.setCurrentStock(5);
        StockAdjustRequest request = new StockAdjustRequest(product.getId(), -10, "Write-off");

        when(productRepository.findByIdAndCompanyId(product.getId(), company.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = stockService.adjustStock(request);

        assertEquals(-5, response.currentStock());
    }

    @Test
    void recordPurchaseIncreasesStock() {
        mockSecurity();
        StockPurchaseRequest request = new StockPurchaseRequest(product.getId(), 30, "Initial stock");

        when(productRepository.findByIdAndCompanyId(product.getId(), company.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = stockService.recordPurchase(request);

        assertNotNull(response);
        assertEquals(80, response.currentStock());
        verify(stockMovementRepository).save(any(StockMovement.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void recordPurchaseThrowsForNonExistentProduct() {
        mockSecurity();
        StockPurchaseRequest request = new StockPurchaseRequest(UUID.randomUUID(), 10, null);

        when(productRepository.findByIdAndCompanyId(request.productId(), company.getId())).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> stockService.recordPurchase(request));
    }

    @Test
    void adjustStockThrowsForNonExistentProduct() {
        mockSecurity();
        StockAdjustRequest request = new StockAdjustRequest(UUID.randomUUID(), 5, "Test");

        when(productRepository.findByIdAndCompanyId(request.productId(), company.getId())).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> stockService.adjustStock(request));
    }
}
