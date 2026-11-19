package com.pasteleria.pos.integration;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** La sección Usuarios solo lista los usuarios de la empresa de quien está logueado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserListCompanyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company centro;
    private String adminToken;

    @BeforeEach
    void setUp() {
        centro = company("Centro");
        Company colon = company("Colon");
        User admin = user("admin-centro", UserRole.SUPER_ADMIN, centro);
        user("vendedora-centro", UserRole.SELLER, centro);
        user("vendedora-colon", UserRole.SELLER, colon);
        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin));
    }

    @Test
    void listsOnlyUsersOfTheLoggedCompany() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].companyId", everyItem(is(centro.getId().toString()))))
                .andExpect(jsonPath("$[*].username", hasItem("vendedora-centro")))
                .andExpect(jsonPath("$[*].username", not(hasItem("vendedora-colon"))));
    }

    @Test
    void developerListsUsersOfAllCompanies() throws Exception {
        User developer = user("dev", UserRole.DEVELOPER, centro);
        String devToken = "Bearer " + jwtService.generateToken(new UserPrincipal(developer));

        mockMvc.perform(get("/api/users").header("Authorization", devToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("vendedora-centro")))
                .andExpect(jsonPath("$[*].username", hasItem("vendedora-colon")));
    }

    @Test
    void nobodyCanCreateDeveloper() throws Exception {
        User developer = user("dev2", UserRole.DEVELOPER, centro);
        for (String token : new String[] {adminToken, "Bearer " + jwtService.generateToken(new UserPrincipal(developer))}) {
            mockMvc.perform(post("/api/users").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson("nuevo-dev", "DEVELOPER", "secreta")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void cannotPromoteToDeveloperButDeveloperKeepsRoleOnEdit() throws Exception {
        User seller = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals("vendedora-centro")).findFirst().orElseThrow();
        mockMvc.perform(put("/api/users/" + seller.getId()).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("vendedora-centro", "DEVELOPER", null)))
                .andExpect(status().isForbidden());

        User developer = user("dev3", UserRole.DEVELOPER, centro);
        mockMvc.perform(put("/api/users/" + developer.getId()).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("dev3", "DEVELOPER", null)))
                .andExpect(status().isOk());
    }

    private String userJson(String username, String role, String password) {
        return """
                {"name":"%s","username":"%s","password":%s,"role":"%s","active":true,"companyId":"%s"}
                """.formatted(username, username, password == null ? "null" : "\"" + password + "\"", role, centro.getId());
    }

    private Company company(String name) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setActive(true);
        return companyRepository.save(company);
    }

    private User user(String username, UserRole role, Company company) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(username);
        user.setUsername(username);
        user.setPasswordHash("pwd");
        user.setRole(role);
        user.setActive(true);
        user.setCompany(company);
        return userRepository.save(user);
    }
}
