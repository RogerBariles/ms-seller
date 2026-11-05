package com.pasteleria.pos.service;

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
import com.pasteleria.pos.dto.CashFlowResponse;
import com.pasteleria.pos.dto.PayPayableRequest;
import com.pasteleria.pos.dto.PayableRequest;
import com.pasteleria.pos.dto.PayableResponse;
import com.pasteleria.pos.dto.ReceivableResponse;
import com.pasteleria.pos.dto.RecurringPayableRequest;
import com.pasteleria.pos.dto.RecurringPayableResponse;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashFlowService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final int MAX_DAYS = 90;
    // Efectivo y transferencia se acreditan en el momento: nunca quedan por cobrar
    private static final List<PaymentMethod> DELAYED_METHODS =
            List.of(PaymentMethod.TARJETA, PaymentMethod.DEBITO, PaymentMethod.QR, PaymentMethod.PEDIDOSYA);

    private final SaleRepository saleRepository;
    private final PayableRepository payableRepository;
    private final RecurringPayableRepository recurringRepository;
    private final ExpenseRepository expenseRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftCashMovementService cashMovementService;
    private final UserService userService;

    public CashFlowService(
            SaleRepository saleRepository,
            PayableRepository payableRepository,
            RecurringPayableRepository recurringRepository,
            ExpenseRepository expenseRepository,
            ShiftRepository shiftRepository,
            ShiftCashMovementService cashMovementService,
            UserService userService) {
        this.saleRepository = saleRepository;
        this.payableRepository = payableRepository;
        this.recurringRepository = recurringRepository;
        this.expenseRepository = expenseRepository;
        this.shiftRepository = shiftRepository;
        this.cashMovementService = cashMovementService;
        this.userService = userService;
    }

    @Transactional
    public CashFlowResponse getCashFlow(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El período debe ser de 1 a " + MAX_DAYS + " días");
        }
        UUID companyId = requiredCompanyId();
        LocalDate today = today();
        LocalDate until = today.plusDays(days);
        generateRecurringPayables(companyId, until);

        List<ReceivableResponse> receivables =
                saleRepository.findReceivables(companyId, today, until, DELAYED_METHODS);
        List<Payable> payables = payableRepository.findByCompanyIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(
                companyId, PayableStatus.PENDING, until);

        BigDecimal totalReceivable = receivables.stream()
                .map(ReceivableResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = sumAmounts(payables);
        BigDecimal totalOverdue = sumAmounts(payables.stream().filter(p -> p.getDueDate().isBefore(today)).toList());

        return new CashFlowResponse(
                today,
                until,
                receivables,
                totalReceivable,
                payables.stream().map(this::toResponse).toList(),
                totalPayable,
                totalOverdue,
                totalReceivable.subtract(totalPayable));
    }

    @Transactional
    public PayableResponse createPayable(PayableRequest request) {
        requiredCompanyId();
        User user = currentUser();
        Payable payable = new Payable();
        payable.setId(UUID.randomUUID());
        payable.setDetail(request.detail().trim());
        payable.setAmount(request.amount());
        payable.setDueDate(request.dueDate());
        payable.setCompany(user.getCompany());
        payable.setCreatedBy(user);
        return toResponse(payableRepository.save(payable));
    }

    @Transactional
    public PayableResponse payPayable(UUID id, PayPayableRequest request) {
        Payable payable = getPendingPayable(id);
        User user = currentUser();
        OffsetDateTime now = OffsetDateTime.now(ZONE);

        if (request.paymentSource() == ExpensePaymentSource.CASH) {
            payFromOpenShift(payable, user);
        } else {
            Expense expense = new Expense(UUID.randomUUID(), payable.getDetail(), payable.getAmount(), user);
            expense.setCompany(payable.getCompany());
            expense.setCreatedAt(now);
            expense.setPaymentSource(request.paymentSource());
            expenseRepository.save(expense);
        }

        payable.setStatus(PayableStatus.PAID);
        payable.setPaymentSource(request.paymentSource());
        payable.setPaidAt(now);
        payable.setPaidBy(user);
        return toResponse(payableRepository.save(payable));
    }

    @Transactional
    public PayableResponse cancelPayable(UUID id) {
        Payable payable = getPendingPayable(id);
        payable.setStatus(PayableStatus.CANCELLED);
        return toResponse(payableRepository.save(payable));
    }

    @Transactional(readOnly = true)
    public List<RecurringPayableResponse> listRecurring() {
        return recurringRepository.findByCompanyIdOrderByDayOfMonthAsc(requiredCompanyId()).stream()
                .map(CashFlowService::toResponse)
                .toList();
    }

    @Transactional
    public RecurringPayableResponse createRecurring(RecurringPayableRequest request) {
        requiredCompanyId();
        User user = currentUser();
        RecurringPayable recurring = new RecurringPayable();
        recurring.setId(UUID.randomUUID());
        recurring.setStartDate(today());
        recurring.setCompany(user.getCompany());
        recurring.setCreatedBy(user);
        applyRecurring(recurring, request);
        return toResponse(recurringRepository.save(recurring));
    }

    @Transactional
    public RecurringPayableResponse updateRecurring(UUID id, RecurringPayableRequest request) {
        RecurringPayable recurring = recurringRepository.findByIdAndCompanyId(id, requiredCompanyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gasto fijo no encontrado"));
        applyRecurring(recurring, request);
        return toResponse(recurringRepository.save(recurring));
    }

    /** Crea las cuentas de cada gasto fijo hasta la fecha pedida; las ya generadas no se repiten. */
    private void generateRecurringPayables(UUID companyId, LocalDate until) {
        for (RecurringPayable recurring : recurringRepository.findByCompanyIdAndActiveTrue(companyId)) {
            YearMonth lastMonth = YearMonth.from(until);
            for (YearMonth month = YearMonth.from(recurring.getStartDate());
                    !month.isAfter(lastMonth);
                    month = month.plusMonths(1)) {
                LocalDate dueDate = month.atDay(Math.min(recurring.getDayOfMonth(), month.lengthOfMonth()));
                if (dueDate.isBefore(recurring.getStartDate())
                        || dueDate.isAfter(until)
                        || payableRepository.existsByRecurringIdAndPeriod(recurring.getId(), month.atDay(1))) {
                    continue;
                }
                Payable payable = new Payable();
                payable.setId(UUID.randomUUID());
                payable.setDetail(recurring.getDetail());
                payable.setAmount(recurring.getAmount());
                payable.setDueDate(dueDate);
                payable.setRecurringId(recurring.getId());
                payable.setPeriod(month.atDay(1));
                payable.setCompany(recurring.getCompany());
                payable.setCreatedBy(recurring.getCreatedBy());
                payableRepository.save(payable);
            }
        }
    }

    /** El pago en efectivo sale de la caja: el retiro descuenta el efectivo del turno y crea el egreso. */
    private void payFromOpenShift(Payable payable, User user) {
        List<Shift> openShifts = shiftRepository.findAllByStatusOrderByStartedAtAsc(
                ShiftStatus.OPEN, payable.getCompany().getId());
        Shift shift = openShifts.stream()
                .filter(s -> s.getSeller().getId().equals(user.getId()))
                .findFirst()
                .or(() -> openShifts.stream().findFirst())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT, "Para pagar en efectivo tiene que haber un turno abierto"));
        cashMovementService.addMovement(shift.getId(), new ShiftCashMovementRequest(
                CashMovementType.WITHDRAWAL, CashMovementCategory.EXPENSE, payable.getAmount(), payable.getDetail()));
    }

    private Payable getPendingPayable(UUID id) {
        Payable payable = payableRepository.findByIdAndCompanyId(id, requiredCompanyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cuenta a pagar no encontrada"));
        if (payable.getStatus() != PayableStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "La cuenta ya no está pendiente");
        }
        return payable;
    }

    private static void applyRecurring(RecurringPayable recurring, RecurringPayableRequest request) {
        recurring.setDetail(request.detail().trim());
        recurring.setAmount(request.amount());
        recurring.setDayOfMonth(request.dayOfMonth());
        recurring.setActive(request.active());
    }

    private static BigDecimal sumAmounts(List<Payable> payables) {
        return payables.stream()
                .map(Payable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PayableResponse toResponse(Payable payable) {
        return new PayableResponse(
                payable.getId(),
                payable.getDetail(),
                payable.getAmount(),
                payable.getDueDate(),
                payable.getStatus(),
                payable.getStatus() == PayableStatus.PENDING && payable.getDueDate().isBefore(today()),
                payable.getRecurringId() != null);
    }

    private static RecurringPayableResponse toResponse(RecurringPayable recurring) {
        return new RecurringPayableResponse(
                recurring.getId(),
                recurring.getDetail(),
                recurring.getAmount(),
                recurring.getDayOfMonth(),
                recurring.isActive());
    }

    private User currentUser() {
        return userService.getUserEntity(SecurityUtils.currentUser().getId());
    }

    private static UUID requiredCompanyId() {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getCompanyId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario debe estar asociado a una empresa");
        }
        return principal.getCompanyId();
    }

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
