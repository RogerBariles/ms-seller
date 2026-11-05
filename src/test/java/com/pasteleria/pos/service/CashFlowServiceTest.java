package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Expense;
import com.pasteleria.pos.domain.entity.Payable;
import com.pasteleria.pos.domain.entity.RecurringPayable;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashMovementCategory;
import com.pasteleria.pos.domain.enums.CashMovementType;
import com.pasteleria.pos.domain.enums.ExpensePaymentSource;
import com.pasteleria.pos.domain.enums.PayableStatus;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.CashFlowResponse;
import com.pasteleria.pos.dto.PayPayableRequest;
import com.pasteleria.pos.dto.ReceivableResponse;
import com.pasteleria.pos.dto.ShiftCashMovementRequest;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.ExpenseRepository;
import com.pasteleria.pos.repository.PayableRepository;
import com.pasteleria.pos.repository.RecurringPayableRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class CashFlowServiceTest {

    private static final UUID PAYABLE_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));

    private SaleRepository saleRepository;
    private PayableRepository payableRepository;
    private RecurringPayableRepository recurringRepository;
    private ExpenseRepository expenseRepository;
    private ShiftRepository shiftRepository;
    private ShiftCashMovementService cashMovementService;
    private CashFlowService service;
    private Company company;
    private User user;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        payableRepository = mock(PayableRepository.class);
        recurringRepository = mock(RecurringPayableRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        shiftRepository = mock(ShiftRepository.class);
        cashMovementService = mock(ShiftCashMovementService.class);
        UserService userService = mock(UserService.class);
        service = new CashFlowService(
                saleRepository, payableRepository, recurringRepository, expenseRepository,
                shiftRepository, cashMovementService, userService);

        company = new Company();
        company.setId(UUID.randomUUID());
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setPasswordHash("pwd");
        user.setRole(UserRole.SUPER_ADMIN);
        user.setActive(true);
        user.setCompany(company);

        when(userService.getUserEntity(user.getId())).thenReturn(user);
        when(payableRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(saleRepository.findReceivables(any(), any(), any(), any())).thenReturn(List.of());
        when(recurringRepository.findByCompanyIdAndActiveTrue(company.getId())).thenReturn(List.of());
        when(payableRepository.findByCompanyIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(any(), any(), any()))
                .thenReturn(List.of());

        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(new UserPrincipal(user));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private Payable pendingPayable(String amount, LocalDate dueDate) {
        Payable payable = new Payable();
        payable.setId(PAYABLE_ID);
        payable.setDetail("Alquiler");
        payable.setAmount(new BigDecimal(amount));
        payable.setDueDate(dueDate);
        payable.setCompany(company);
        when(payableRepository.findByIdAndCompanyId(PAYABLE_ID, company.getId())).thenReturn(Optional.of(payable));
        return payable;
    }

    private RecurringPayable recurring(int day, LocalDate startDate) {
        RecurringPayable recurring = new RecurringPayable();
        recurring.setId(UUID.randomUUID());
        recurring.setDetail("Alquiler");
        recurring.setAmount(new BigDecimal("1000.00"));
        recurring.setDayOfMonth(day);
        recurring.setStartDate(startDate);
        recurring.setCompany(company);
        recurring.setCreatedBy(user);
        when(recurringRepository.findByCompanyIdAndActiveTrue(company.getId())).thenReturn(List.of(recurring));
        return recurring;
    }

    @Test
    void cashFlowTotalsReceivablesPayablesAndOverdue() {
        when(saleRepository.findReceivables(eq(company.getId()), eq(TODAY), eq(TODAY.plusDays(30)), any()))
                .thenReturn(List.of(new ReceivableResponse(
                        TODAY.plusDays(2), PaymentMethod.TARJETA, new BigDecimal("500.00"))));
        Payable overdue = pendingPayable("100.00", TODAY.minusDays(1));
        Payable upcoming = pendingPayable("150.00", TODAY.plusDays(3));
        when(payableRepository.findByCompanyIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(
                company.getId(), PayableStatus.PENDING, TODAY.plusDays(30)))
                .thenReturn(List.of(overdue, upcoming));

        CashFlowResponse response = service.getCashFlow(30);

        assertEquals(new BigDecimal("500.00"), response.totalReceivable());
        assertEquals(new BigDecimal("250.00"), response.totalPayable());
        assertEquals(new BigDecimal("100.00"), response.totalOverdue());
        assertEquals(new BigDecimal("250.00"), response.balance());
        assertEquals(true, response.payables().get(0).overdue());
    }

    @Test
    void rejectsPeriodOutOfRange() {
        assertThrows(ApiException.class, () -> service.getCashFlow(0));
        assertThrows(ApiException.class, () -> service.getCashFlow(91));
    }

    @Test
    void bankPaymentCreatesExpenseAndDoesNotTouchShift() {
        Payable payable = pendingPayable("100.00", TODAY);

        service.payPayable(PAYABLE_ID, new PayPayableRequest(ExpensePaymentSource.BANK));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertEquals(ExpensePaymentSource.BANK, captor.getValue().getPaymentSource());
        assertEquals(company, captor.getValue().getCompany());
        assertEquals(PayableStatus.PAID, payable.getStatus());
        verify(cashMovementService, never()).addMovement(any(), any());
    }

    @Test
    void cashPaymentWithdrawsFromOpenShiftWithoutDuplicatingExpense() {
        Payable payable = pendingPayable("100.00", TODAY);
        Shift other = new Shift();
        other.setId(UUID.randomUUID());
        User otherSeller = new User();
        otherSeller.setId(UUID.randomUUID());
        other.setSeller(otherSeller);
        Shift own = new Shift();
        own.setId(UUID.randomUUID());
        own.setSeller(user);
        when(shiftRepository.findAllByStatusOrderByStartedAtAsc(ShiftStatus.OPEN, company.getId()))
                .thenReturn(List.of(other, own));

        service.payPayable(PAYABLE_ID, new PayPayableRequest(ExpensePaymentSource.CASH));

        ArgumentCaptor<ShiftCashMovementRequest> captor = ArgumentCaptor.forClass(ShiftCashMovementRequest.class);
        verify(cashMovementService).addMovement(eq(own.getId()), captor.capture());
        assertEquals(CashMovementType.WITHDRAWAL, captor.getValue().type());
        assertEquals(CashMovementCategory.EXPENSE, captor.getValue().category());
        verify(expenseRepository, never()).save(any());
        assertEquals(PayableStatus.PAID, payable.getStatus());
    }

    @Test
    void cashPaymentWithoutOpenShiftFailsAndStaysPending() {
        Payable payable = pendingPayable("100.00", TODAY);
        when(shiftRepository.findAllByStatusOrderByStartedAtAsc(ShiftStatus.OPEN, company.getId()))
                .thenReturn(List.of());

        assertThrows(ApiException.class,
                () -> service.payPayable(PAYABLE_ID, new PayPayableRequest(ExpensePaymentSource.CASH)));
        assertEquals(PayableStatus.PENDING, payable.getStatus());
    }

    @Test
    void paidPayableCannotBePaidAgain() {
        Payable payable = pendingPayable("100.00", TODAY);
        payable.setStatus(PayableStatus.PAID);

        assertThrows(ApiException.class,
                () -> service.payPayable(PAYABLE_ID, new PayPayableRequest(ExpensePaymentSource.BANK)));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void recurringUsesLastDayOfShortMonths() {
        LocalDate monthStart = TODAY.withDayOfMonth(1);
        recurring(31, monthStart);
        when(payableRepository.existsByRecurringIdAndPeriod(any(), any())).thenReturn(false);

        service.getCashFlow(90);

        ArgumentCaptor<Payable> captor = ArgumentCaptor.forClass(Payable.class);
        verify(payableRepository, atLeastOnce()).save(captor.capture());
        for (Payable payable : captor.getAllValues()) {
            assertEquals(payable.getDueDate().lengthOfMonth(), payable.getDueDate().getDayOfMonth());
        }
    }

    @Test
    void recurringIsNotGeneratedTwiceForSameMonth() {
        recurring(TODAY.getDayOfMonth(), TODAY);
        when(payableRepository.existsByRecurringIdAndPeriod(any(), any())).thenReturn(true);

        service.getCashFlow(30);

        verify(payableRepository, never()).save(any());
    }

    @Test
    void recurringCreatedAfterItsDayStartsNextMonth() {
        RecurringPayable recurring = recurring(1, TODAY.withDayOfMonth(2));
        when(payableRepository.existsByRecurringIdAndPeriod(any(), any())).thenReturn(false);

        service.getCashFlow(90);

        ArgumentCaptor<Payable> captor = ArgumentCaptor.forClass(Payable.class);
        verify(payableRepository, atLeastOnce()).save(captor.capture());
        List<LocalDate> dueDates = new ArrayList<>(captor.getAllValues().stream().map(Payable::getDueDate).toList());
        assertEquals(TODAY.plusMonths(1).withDayOfMonth(1), dueDates.get(0));
        assertEquals(recurring.getId(), captor.getAllValues().get(0).getRecurringId());
    }
}
