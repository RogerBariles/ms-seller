package com.pasteleria.pos.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserPrincipalTest {

    @Test
    void constructorWithCompanySetsCompanyId() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        company.setName("Test Company");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setName("Test User");
        user.setRole(UserRole.SELLER);
        user.setActive(true);
        user.setCompany(company);

        UserPrincipal principal = new UserPrincipal(user);

        assertNotNull(principal.getCompanyId());
        assertEquals(companyId, principal.getCompanyId());
    }

    @Test
    void constructorWithNullCompanyReturnsNullCompanyId() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("superadmin");
        user.setName("Super Admin");
        user.setRole(UserRole.SUPER_ADMIN);
        user.setActive(true);
        user.setCompany(null);

        UserPrincipal principal = new UserPrincipal(user);

        assertNull(principal.getCompanyId());
    }

    @Test
    void constructorPreservesExistingFields() {
        UUID userId = UUID.randomUUID();
        Company company = new Company();
        company.setId(UUID.randomUUID());

        User user = new User();
        user.setId(userId);
        user.setUsername("seller1");
        user.setName("Seller One");
        user.setPasswordHash("hashed_pwd");
        user.setRole(UserRole.SELLER);
        user.setActive(true);
        user.setCompany(company);

        UserPrincipal principal = new UserPrincipal(user);

        assertEquals(userId, principal.getId());
        assertEquals("seller1", principal.getUsername());
        assertEquals("hashed_pwd", principal.getPassword());
        assertEquals(UserRole.SELLER, principal.getRole());
        assertEquals(true, principal.isEnabled());
    }
}
