package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Expense;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.ShiftCashMovement;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashMovementCategory;
import com.pasteleria.pos.domain.enums.CashMovementType;
import com.pasteleria.pos.domain.enums.ExpensePaymentSource;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.ShiftCashMovementRequest;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.ExpenseRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftCashMovementRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ShiftCashMovementServiceTest {

    private static final UUID SHIFT_ID = UUID.randomUUID();

    private ShiftCashMovementRepository movementRepository;
    private ExpenseRepository expenseRepository;
    private ShiftCashMovementService service;
    private Company company;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        movementRepository = mock(ShiftCashMovementRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        ShiftRepository shiftRepository = mock(ShiftRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        UserService userService = mock(UserService.class);
        service = new ShiftCashMovementService(
                movementRepository, shiftRepository, saleRepository, userService, expenseRepository);

        company = new Company();
        company.setId(UUID.randomUUID());

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("vendedora");
        user.setPasswordHash("pwd");
        user.setName("Vendedora");
        user.setRole(UserRole.SELLER);
        user.setActive(true);
        user.setCompany(company);

        Shift shift = new Shift();
        shift.setId(SHIFT_ID);
        shift.setSeller(user);
        shift.setCompany(company);
        shift.setStatus(ShiftStatus.OPEN);
        shift.setInitialCash(new BigDecimal("1000.00"));

        when(shiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(shift));
        when(userService.getUserEntity(user.getId())).thenReturn(user);
        when(saleRepository.sumCashAmountByShift(SHIFT_ID)).thenReturn(BigDecimal.ZERO);
        when(movementRepository.sumByShiftIdAndType(any(), any())).thenReturn(BigDecimal.ZERO);
        when(movementRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser).thenReturn(new UserPrincipal(user));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private static ShiftCashMovementRequest withdrawal(CashMovementCategory category) {
        return new ShiftCashMovementRequest(
                CashMovementType.WITHDRAWAL, category, new BigDecimal("300.00"), "Harina");
    }

    @Test
    void supplierPaymentCreatesLinkedCashExpense() {
        var response = service.addMovement(SHIFT_ID, withdrawal(CashMovementCategory.SUPPLIER_PAYMENT));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense expense = captor.getValue();
        assertEquals(new BigDecimal("300.00"), expense.getAmount());
        assertEquals("Harina", expense.getDetail());
        assertEquals(ExpensePaymentSource.CASH, expense.getPaymentSource());
        assertEquals(response.id(), expense.getCashMovementId());
        assertEquals(company, expense.getCompany());
    }

    @Test
    void bankDepositDoesNotCreateExpense() {
        service.addMovement(SHIFT_ID, withdrawal(CashMovementCategory.BANK_DEPOSIT));

        verify(movementRepository).save(any(ShiftCashMovement.class));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void rejectsCategoryThatDoesNotMatchType() {
        var request = new ShiftCashMovementRequest(
                CashMovementType.INCOME, CashMovementCategory.BANK_DEPOSIT, BigDecimal.TEN, "Error");

        assertThrows(ApiException.class, () -> service.addMovement(SHIFT_ID, request));
        verify(movementRepository, never()).save(any());
    }
}
