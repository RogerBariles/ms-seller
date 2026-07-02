package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.dto.ProductRequest;
import com.pasteleria.pos.dto.ProductResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.ProductPriceAuditRepository;
import com.pasteleria.pos.repository.ProductRepository;
import com.pasteleria.pos.repository.SaleItemRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductPriceAuditRepository auditRepository;
    private SaleItemRepository saleItemRepository;
    private UserService userService;
    private CompanyService companyService;
    private ProductService productService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        productRepository = mock(ProductRepository.class);
        auditRepository = mock(ProductPriceAuditRepository.class);
        saleItemRepository = mock(SaleItemRepository.class);
        userService = mock(UserService.class);
        companyService = mock(CompanyService.class);
        productService = new ProductService(
                productRepository, auditRepository, saleItemRepository, userService, companyService);
    }

    @Test
    void createProductWithCompany() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        company.setName("Test Company");
        company.setActive(true);

        ProductRequest request = new ProductRequest(
                "New Product",
                ProductCategory.TORTAS,
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                true,
                companyId);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(companyId, response.companyId());
        assertEquals("Test Company", response.companyName());
        verify(companyService).getCompanyEntity(companyId);
    }

    @Test
    void createProductWithInactiveCompanyThrows() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        company.setName("Inactive Company");
        company.setActive(false);

        ProductRequest request = new ProductRequest(
                "New Product",
                ProductCategory.TORTAS,
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                true,
                companyId);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);

        assertThrows(ApiException.class, () -> productService.createProduct(request));
    }

    @Test
    void updateProductChangesCompany() {
        UUID productId = UUID.randomUUID();
        UUID oldCompanyId = UUID.randomUUID();
        UUID newCompanyId = UUID.randomUUID();

        Company oldCompany = new Company();
        oldCompany.setId(oldCompanyId);
        oldCompany.setName("Old Company");
        oldCompany.setActive(true);

        Company newCompany = new Company();
        newCompany.setId(newCompanyId);
        newCompany.setName("New Company");
        newCompany.setActive(true);

        Product existingProduct = new Product();
        existingProduct.setId(productId);
        existingProduct.setName("Product");
        existingProduct.setCategory(ProductCategory.TORTAS);
        existingProduct.setPrice(new BigDecimal("100.00"));
        existingProduct.setPurchasePrice(new BigDecimal("50.00"));
        existingProduct.setActive(true);
        existingProduct.setCompany(oldCompany);

        // Use same prices to avoid triggering price audit (which needs SecurityUtils)
        ProductRequest request = new ProductRequest(
                "Updated Product",
                ProductCategory.TORTAS,
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                true,
                newCompanyId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(companyService.getCompanyEntity(newCompanyId)).thenReturn(newCompany);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct(productId, request);

        assertNotNull(response);
        assertEquals(newCompanyId, response.companyId());
        assertEquals("New Company", response.companyName());
    }

    @Test
    void createProductWithNonExistentCompanyThrows() {
        UUID companyId = UUID.randomUUID();

        ProductRequest request = new ProductRequest(
                "New Product",
                ProductCategory.TORTAS,
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                true,
                companyId);

        when(companyService.getCompanyEntity(companyId))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        assertThrows(ApiException.class, () -> productService.createProduct(request));
    }
}
