package com.pasteleria.pos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ReportServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID OTHER_SELLER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    private SaleRepository saleRepository;
    private ReportService reportService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        reportService = new ReportService(saleRepository);
        when(saleRepository.findForReport(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Test
    void sellerReportIgnoresRequestedSellerIdAndUsesLoggedInUser() {
        UserPrincipal principal = new UserPrincipal(createUser(SELLER_ID, UserRole.SELLER));
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(principal);

        LocalDate today = LocalDate.now();
        reportService.getSalesReport(today, today, null, OTHER_SELLER_ID, COMPANY_ID, null);

        verify(saleRepository).findForReport(any(), any(), isNull(), eq(SELLER_ID), eq(COMPANY_ID), isNull());
    }

    @Test
    void superAdminReportKeepsRequestedSellerId() {
        UserPrincipal principal = new UserPrincipal(createUser(UUID.randomUUID(), UserRole.SUPER_ADMIN));
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(principal);

        LocalDate today = LocalDate.now();
        reportService.getSalesReport(today, today, null, OTHER_SELLER_ID, COMPANY_ID, null);

        verify(saleRepository).findForReport(any(), any(), isNull(), eq(OTHER_SELLER_ID), eq(COMPANY_ID), isNull());
    }

    private static User createUser(UUID id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName(role.name());
        user.setUsername(role.name().toLowerCase());
        user.setRole(role);
        user.setActive(true);
        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Company");
        company.setActive(true);
        user.setCompany(company);
        return user;
    }
}
