package com.pasteleria.pos.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.dto.ProductResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtoMapperTest {

    @Test
    void toProductResponseWithCompany() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setCategory(ProductCategory.TORTAS);
        product.setPrice(new BigDecimal("100.00"));
        product.setPurchasePrice(new BigDecimal("50.00"));
        product.setActive(true);
        product.setUpdatedAt(OffsetDateTime.now());
        product.setCompany(company);

        ProductResponse response = DtoMapper.toProductResponse(product);

        assertNotNull(response);
        assertEquals(product.getId(), response.id());
        assertEquals(company.getId(), response.companyId());
        assertEquals(company.getName(), response.companyName());
    }

    @Test
    void toProductResponseWithNullCompany() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Birthday Product");
        product.setCategory(ProductCategory.TORTAS);
        product.setPrice(new BigDecimal("0.00"));
        product.setPurchasePrice(new BigDecimal("0.00"));
        product.setActive(true);
        product.setUpdatedAt(OffsetDateTime.now());
        product.setCompany(null);

        ProductResponse response = DtoMapper.toProductResponse(product);

        assertNotNull(response);
        assertEquals(product.getId(), response.id());
        assertNull(response.companyId());
        assertNull(response.companyName());
    }
}
