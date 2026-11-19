package com.pasteleria.pos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.SaleItem;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.service.SaleService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * GET /sales/{id} (lo usa "Anular venta") se llama sin transacción abierta y con open-in-view desactivado:
 * el servicio tiene que poder cargar los ítems por su cuenta. Este test no es @Transactional a propósito.
 */
@SpringBootTest
@ActiveProfiles("test")
class GetSaleOutsideTransactionTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate tx;

    private UUID companyId;
    private UUID userId;
    private UUID productId;
    private UUID saleId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            Company company = new Company();
            company.setId(UUID.randomUUID());
            company.setName("Lazy Co");
            company.setActive(true);
            em.persist(company);
            companyId = company.getId();

            User user = new User();
            user.setId(UUID.randomUUID());
            user.setName("Vendedora");
            user.setUsername("lazy-" + UUID.randomUUID());
            user.setPasswordHash("pwd");
            user.setRole(UserRole.SELLER);
            user.setActive(true);
            user.setCompany(company);
            em.persist(user);
            userId = user.getId();

            Product product = new Product();
            product.setId(UUID.randomUUID());
            product.setName("Torta");
            product.setCategory(ProductCategory.TORTAS);
            product.setPrice(new BigDecimal("100.00"));
            product.setPurchasePrice(new BigDecimal("50.00"));
            product.setActive(true);
            product.setCompany(company);
            product.setCreatedAt(OffsetDateTime.now());
            product.setUpdatedAt(OffsetDateTime.now());
            em.persist(product);
            productId = product.getId();

            Sale sale = new Sale();
            sale.setId(UUID.randomUUID());
            sale.setSeller(user);
            sale.setCompany(company);
            sale.setPaymentMethod(PaymentMethod.EFECTIVO);
            sale.setSubtotal(new BigDecimal("200.00"));
            sale.setDiscountTotal(BigDecimal.ZERO);
            sale.setTotal(new BigDecimal("200.00"));
            sale.setCashAmount(new BigDecimal("200.00"));
            sale.setAccreditedAt(LocalDate.now());

            SaleItem item = new SaleItem();
            item.setId(UUID.randomUUID());
            item.setSale(sale);
            item.setProduct(product);
            item.setProductName("Torta");
            item.setQuantity(2);
            item.setUnitPrice(new BigDecimal("100.00"));
            item.setUnitPurchasePrice(new BigDecimal("50.00"));
            item.setLineSubtotal(new BigDecimal("200.00"));
            item.setLineDiscount(BigDecimal.ZERO);
            item.setLineTotal(new BigDecimal("200.00"));
            sale.getItems().add(item);
            em.persist(sale);
            saleId = sale.getId();
        });
    }

    @AfterEach
    void tearDown() {
        tx.executeWithoutResult(status -> {
            em.createNativeQuery("DELETE FROM sale_items WHERE sale_id = :id").setParameter("id", saleId).executeUpdate();
            em.createNativeQuery("DELETE FROM sales WHERE id = :id").setParameter("id", saleId).executeUpdate();
            em.createNativeQuery("DELETE FROM products WHERE id = :id").setParameter("id", productId).executeUpdate();
            em.createNativeQuery("DELETE FROM users WHERE id = :id").setParameter("id", userId).executeUpdate();
            em.createNativeQuery("DELETE FROM companies WHERE id = :id").setParameter("id", companyId).executeUpdate();
        });
    }

    @Test
    void getSaleLoadsItemsWithoutOuterTransaction() {
        SaleResponse response = saleService.getSale(saleId);

        assertEquals(1, response.items().size());
        assertEquals("Torta", response.items().get(0).productName());
    }
}
