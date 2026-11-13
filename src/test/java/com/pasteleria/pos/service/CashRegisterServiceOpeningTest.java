package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.CashRegisterResponse;
import com.pasteleria.pos.dto.OpenCashRegisterRequest;
import com.pasteleria.pos.dto.OpeningCashResponse;
import com.pasteleria.pos.dto.PaymentTotalsResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.CashRegisterRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

class CashRegisterServiceOpeningTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    private CashRegisterRepository cashRegisterRepository;
    private CloseReportBuilder closeReportBuilder;
    private ShiftCashMovementService cashMovementService;
    private CashRegisterService service;
    private MockedStatic<SecurityUtils> securityUtilsMock;
    private User user;

    @BeforeEach
    void setUp() {
        cashRegisterRepository = mock(CashRegisterRepository.class);
        closeReportBuilder = mock(CloseReportBuilder.class);
        cashMovementService = mock(ShiftCashMovementService.class);
        UserService userService = mock(UserService.class);
        service = new CashRegisterService(
                cashRegisterRepository,
                mock(ShiftRepository.class),
                mock(SaleRepository.class),
                userService,
                closeReportBuilder,
                mock(CloseReportService.class),
                cashMovementService);

        user = new User();
        user.setId(USER_ID);
        user.setName("Rocío");
        user.setUsername("rocio");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Centro");
        user.setCompany(company);
        when(userService.getUserEntity(USER_ID)).thenReturn(user);

        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(new UserPrincipal(user));

        when(cashRegisterRepository.save(any(CashRegister.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void opensWithFinalCashOfLastClosedRegisterIgnoringRequest() {
        CashRegister previous = closedRegister(new BigDecimal("10000"));
        givenLastClosed(previous);
        BigDecimal cashSales = new BigDecimal("25000");
        when(closeReportBuilder.paymentTotalsByCashRegister(previous.getId())).thenReturn(cashTotals(cashSales));
        when(cashMovementService.expectedFinalCashForCashRegister(previous.getId(), previous.getInitialCash(), cashSales))
                .thenReturn(new BigDecimal("31500"));

        CashRegisterResponse opened = service.openCashRegister(new OpenCashRegisterRequest(new BigDecimal("999")));

        assertEquals(new BigDecimal("31500"), opened.initialCash());
    }

    @Test
    void firstRegisterUsesRequestAmount() {
        givenLastClosed(null);

        CashRegisterResponse opened = service.openCashRegister(new OpenCashRegisterRequest(new BigDecimal("5000")));

        assertEquals(new BigDecimal("5000"), opened.initialCash());
    }

    @Test
    void firstRegisterWithoutAmountFails() {
        givenLastClosed(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.openCashRegister(null));
        assertTrue(ex.getMessage().contains("efectivo inicial"));
    }

    @Test
    void openingCashReportsPreviousClose() {
        CashRegister previous = closedRegister(new BigDecimal("2000"));
        givenLastClosed(previous);
        when(closeReportBuilder.paymentTotalsByCashRegister(previous.getId())).thenReturn(cashTotals(BigDecimal.ZERO));
        when(cashMovementService.expectedFinalCashForCashRegister(eq(previous.getId()), any(), any()))
                .thenReturn(new BigDecimal("2000"));

        OpeningCashResponse response = service.getOpeningCash();

        assertTrue(response.fromPreviousClose());
        assertEquals(new BigDecimal("2000"), response.initialCash());
        assertEquals(previous.getBusinessDate(), response.previousBusinessDate());
        assertEquals("Rocío", response.previousClosedByName());
    }

    @Test
    void openingCashWithoutPreviousClose() {
        givenLastClosed(null);

        OpeningCashResponse response = service.getOpeningCash();

        assertFalse(response.fromPreviousClose());
        assertEquals(BigDecimal.ZERO, response.initialCash());
    }

    private void givenLastClosed(CashRegister register) {
        when(cashRegisterRepository.findClosedByCompanyOrderByClosedAtDesc(eq(COMPANY_ID), any(Pageable.class)))
                .thenReturn(register == null ? List.of() : List.of(register));
    }

    private CashRegister closedRegister(BigDecimal initialCash) {
        CashRegister register = new CashRegister();
        register.setId(UUID.randomUUID());
        register.setBusinessDate(LocalDate.now().minusDays(1));
        register.setInitialCash(initialCash);
        register.setStatus(CashRegisterStatus.CLOSED);
        register.setOpenedBy(user);
        register.setClosedBy(user);
        register.setCompany(user.getCompany());
        register.setOpenedAt(OffsetDateTime.now().minusDays(1));
        register.setClosedAt(OffsetDateTime.now().minusHours(12));
        return register;
    }

    private static PaymentTotalsResponse cashTotals(BigDecimal cash) {
        BigDecimal z = BigDecimal.ZERO;
        return new PaymentTotalsResponse(cash, z, z, z, z, z);
    }
}
