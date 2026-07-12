package com.pasteleria.pos.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.StockAdjustRequest;
import com.pasteleria.pos.dto.StockPurchaseRequest;
import com.pasteleria.pos.repository.CompanyRepository;
import com.pasteleria.pos.repository.ProductRepository;
import com.pasteleria.pos.repository.UserRepository;
import com.pasteleria.pos.security.JwtService;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ProductRepository productRepository;

    private String jwtToken;
    private UUID productId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        company.setActive(true);
        company = companyRepository.save(company);
        companyId = company.getId();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setUsername("stocktest");
        user.setPasswordHash("pwd");
        user.setRole(UserRole.ADMIN);
        user.setCompany(company);
        user.setActive(true);
        user = userRepository.save(user);

        jwtToken = "Bearer " + jwtService.generateToken(new UserPrincipal(user));

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Stock Test Product");
        product.setCategory(ProductCategory.TORTAS);
        product.setPrice(new BigDecimal("100.00"));
        product.setPurchasePrice(new BigDecimal("50.00"));
        product.setActive(true);
        product.setCurrentStock(20);
        product.setCompany(company);
        product.setCreatedAt(OffsetDateTime.now());
        product.setUpdatedAt(OffsetDateTime.now());
        product = productRepository.save(product);
        productId = product.getId();
    }

    @Test
    void getCurrentStockReturnsStock() throws Exception {
        mockMvc.perform(get("/api/stock/product/{productId}", productId)
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.productName").value("Stock Test Product"))
                .andExpect(jsonPath("$.currentStock").value(20));
    }

    @Test
    void adjustStockReturnsUpdatedStock() throws Exception {
        StockAdjustRequest request = new StockAdjustRequest(productId, -5, "Damaged goods");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/stock/adjust")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStock").value(15));
    }

    @Test
    void recordPurchaseIncreasesStock() throws Exception {
        StockPurchaseRequest request = new StockPurchaseRequest(productId, 30, "Initial stock");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/stock/purchase")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStock").value(50));
    }

    @Test
    void getStockAfterMultipleMovementsReflectsChanges() throws Exception {
        StockAdjustRequest adj = new StockAdjustRequest(productId, -10, "Write-off");
        mockMvc.perform(post("/api/stock/adjust")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adj)))
                .andExpect(status().isCreated());

        StockPurchaseRequest purch = new StockPurchaseRequest(productId, 5, "Re-stock");
        mockMvc.perform(post("/api/stock/purchase")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purch)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/stock/product/{productId}", productId)
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(15));
    }

    @Test
    void negativeStockIsAllowed() throws Exception {
        StockAdjustRequest request = new StockAdjustRequest(productId, -30, "Heavy loss");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/stock/adjust")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStock").value(-10));
    }

    @Test
    void getCurrentStockReturnsNotFoundForOtherCompany() throws Exception {
        // Create another company with an unrelated product
        Company otherCompany = new Company();
        otherCompany.setId(UUID.randomUUID());
        otherCompany.setName("Other Company");
        otherCompany.setActive(true);
        otherCompany = companyRepository.save(otherCompany);

        Product otherProduct = new Product();
        otherProduct.setId(UUID.randomUUID());
        otherProduct.setName("Other Product");
        otherProduct.setCategory(ProductCategory.TORTAS);
        otherProduct.setPrice(new BigDecimal("50.00"));
        otherProduct.setPurchasePrice(new BigDecimal("25.00"));
        otherProduct.setActive(true);
        otherProduct.setCurrentStock(100);
        otherProduct.setCompany(otherCompany);
        otherProduct.setCreatedAt(OffsetDateTime.now());
        otherProduct.setUpdatedAt(OffsetDateTime.now());
        otherProduct = productRepository.save(otherProduct);

        // Use first company's token — should NOT see other company's stock
        mockMvc.perform(get("/api/stock/product/{productId}", otherProduct.getId())
                        .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

}
