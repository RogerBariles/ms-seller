package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.ShiftCashMovement;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashMovementCategory;
import com.pasteleria.pos.domain.enums.CashMovementType;
import com.pasteleria.pos.repository.ExpenseRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftCashMovementRepository;
import com.pasteleria.pos.repository.UserRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ContabilidadServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();

    private SaleRepository saleRepository;
    private ExpenseRepository expenseRepository;
    private ShiftCashMovementRepository movementRepository;
    private ContabilidadService service;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        movementRepository = mock(ShiftCashMovementRepository.class);
        service = new ContabilidadService(
                saleRepository, expenseRepository, movementRepository,
                mock(UserRepository.class), mock(CompanyService.class));

        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getCompanyId()).thenReturn(COMPANY_ID);
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(principal);

        when(saleRepository.sumSalesBetweenByCompany(any(), any(), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("1000.00"));
        when(expenseRepository.findBetweenByCompany(any(), any(), eq(COMPANY_ID))).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private static ShiftCashMovement movement(CashMovementType type, CashMovementCategory category, String amount) {
        User user = new User();
        user.setName("Vendedora");
        ShiftCashMovement movement = new ShiftCashMovement();
        movement.setId(UUID.randomUUID());
        movement.setMovementType(type);
        movement.setCategory(category);
        movement.setAmount(new BigDecimal(amount));
        movement.setDetail("detalle");
        movement.setCreatedBy(user);
        return movement;
    }

    private BigDecimal netAmount(String expenses, ShiftCashMovement... movements) {
        when(expenseRepository.sumExpensesBetweenByCompany(any(), any(), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal(expenses));
        when(movementRepository.findBetweenByCompany(any(), any(), eq(COMPANY_ID)))
                .thenReturn(List.of(movements));
        LocalDate today = LocalDate.of(2026, 11, 5);
        return service.getSummary(today, today).netAmount();
    }

    @Test
    void withdrawalThatCreatedAnExpenseIsNotSubtractedTwice() {
        BigDecimal net = netAmount("100.00",
                movement(CashMovementType.WITHDRAWAL, CashMovementCategory.SUPPLIER_PAYMENT, "100.00"));

        assertEquals(new BigDecimal("900.00"), net);
    }

    @Test
    void bankDepositAndChangeFundDoNotAffectResult() {
        BigDecimal net = netAmount("0.00",
                movement(CashMovementType.WITHDRAWAL, CashMovementCategory.BANK_DEPOSIT, "400.00"),
                movement(CashMovementType.INCOME, CashMovementCategory.CHANGE_FUND, "50.00"));

        assertEquals(new BigDecimal("1000.00"), net);
    }

    @Test
    void ownerWithdrawalAndLegacyMovementsKeepAffectingResult() {
        BigDecimal net = netAmount("0.00",
                movement(CashMovementType.WITHDRAWAL, CashMovementCategory.OWNER_WITHDRAWAL, "200.00"),
                movement(CashMovementType.WITHDRAWAL, CashMovementCategory.OTHER, "100.00"),
                movement(CashMovementType.INCOME, CashMovementCategory.OTHER, "10.00"));

        assertEquals(new BigDecimal("710.00"), net);
    }
}
