package com.pasteleria.pos.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.repository.CompanyRepository;
import com.pasteleria.pos.repository.UserRepository;
import com.pasteleria.pos.security.JwtService;
import com.pasteleria.pos.security.UserPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Alta y búsqueda por DNI de clientes (modal "Asociar cliente" en Ventas). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private String token;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Centro");
        company.setActive(true);
        company = companyRepository.save(company);

        User seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setName("Vendedora");
        seller.setUsername("vendedora-clientes");
        seller.setPasswordHash("pwd");
        seller.setRole(UserRole.SELLER);
        seller.setActive(true);
        seller.setCompany(company);
        seller = userRepository.save(seller);
        token = "Bearer " + jwtService.generateToken(new UserPrincipal(seller));
    }

    @Test
    void searchNotFoundThenCreateThenFound() throws Exception {
        mockMvc.perform(get("/api/customers/by-dni").param("dni", "30111222").header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("30111222", "1151234567", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("30111222"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mockMvc.perform(get("/api/customers/by-dni").param("dni", "30111222").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Pérez"));
    }

    @Test
    void requiresWhatsappOrEmail() throws Exception {
        mockMvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("30111223", "", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailAloneIsEnough() throws Exception {
        mockMvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("30111224", "", "Ana@Mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@mail.com"));
    }

    @Test
    void dniNeedsAtLeastEightDigits() throws Exception {
        mockMvc.perform(get("/api/customers/by-dni").param("dni", "1234567").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("1234567", "1151234567", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateDniIsRejected() throws Exception {
        mockMvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("30111225", "1151234567", "")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("30111225", "1151234567", "")))
                .andExpect(status().isConflict());
    }

    private static String json(String dni, String whatsapp, String email) {
        return """
                {"fullName":"Ana Pérez","dni":"%s","birthDate":"1990-05-10","whatsapp":"%s","email":"%s"}
                """.formatted(dni, whatsapp, email);
    }
}
