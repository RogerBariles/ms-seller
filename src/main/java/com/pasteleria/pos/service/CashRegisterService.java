package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.dto.CashRegisterActiveResponse;
import com.pasteleria.pos.dto.CashRegisterResponse;
import com.pasteleria.pos.dto.CloseReportResponse;
import com.pasteleria.pos.dto.OpenCashRegisterRequest;
import com.pasteleria.pos.dto.PaymentTotalsResponse;
import com.pasteleria.pos.dto.ShiftCashMovementResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.CashRegisterRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashRegisterService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final CashRegisterRepository cashRegisterRepository;
    private final ShiftRepository shiftRepository;
    private final SaleRepository saleRepository;
    private final UserService userService;
    private final CloseReportBuilder closeReportBuilder;
    private final ShiftCashMovementService cashMovementService;

    public CashRegisterService(
            CashRegisterRepository cashRegisterRepository,
            ShiftRepository shiftRepository,
            SaleRepository saleRepository,
            UserService userService,
            CloseReportBuilder closeReportBuilder,
            ShiftCashMovementService cashMovementService) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.shiftRepository = shiftRepository;
        this.saleRepository = saleRepository;
        this.userService = userService;
        this.closeReportBuilder = closeReportBuilder;
        this.cashMovementService = cashMovementService;
    }

    @Transactional(readOnly = true)
    public Optional<CashRegisterActiveResponse> getTodayCashRegister() {
        return findOpenCashRegisterForToday()
                .map(this::toActiveResponse);
    }

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> getTodayCashRegisterHistory() {
        return cashRegisterRepository.findAllByBusinessDateOrderByOpenedAtDesc(today()).stream()
                .map(DtoMapper::toCashRegisterResponse)
                .toList();
    }

    @Transactional
    public CashRegisterResponse openCashRegister(OpenCashRegisterRequest request) {
        LocalDate today = today();
        if (cashRegisterRepository.existsByBusinessDateAndStatus(today, CashRegisterStatus.OPEN)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ya hay una caja abierta. Cierre la caja actual antes de abrir otra.");
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());

        CashRegister cashRegister = new CashRegister();
        cashRegister.setId(UUID.randomUUID());
        cashRegister.setBusinessDate(today);
        cashRegister.setInitialCash(request.initialCash());
        cashRegister.setStatus(CashRegisterStatus.OPEN);
        cashRegister.setOpenedBy(user);
        cashRegister.setOpenedAt(OffsetDateTime.now(ZONE));
        return DtoMapper.toCashRegisterResponse(cashRegisterRepository.save(cashRegister));
    }

    @Transactional
    public CloseReportResponse closeCashRegister(UUID id) {
        CashRegister cashRegister = cashRegisterRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caja no encontrada"));
        if (cashRegister.getStatus() == CashRegisterStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "La caja ya está cerrada");
        }
        if (shiftRepository.existsByCashRegisterIdAndStatus(cashRegister.getId(), ShiftStatus.OPEN)) {
            throw new ApiException(HttpStatus.CONFLICT, "Hay turnos abiertos. Cierre todos los turnos primero");
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        OffsetDateTime closedAt = OffsetDateTime.now(ZONE);
        cashRegister.setStatus(CashRegisterStatus.CLOSED);
        cashRegister.setClosedBy(user);
        cashRegister.setClosedAt(closedAt);
        cashRegisterRepository.save(cashRegister);

        PaymentTotalsResponse paymentTotals = closeReportBuilder.paymentTotalsByCashRegister(cashRegister.getId());
        long salesCount = saleRepository.countByCashRegisterId(cashRegister.getId());
        BigDecimal totalSales = CloseReportBuilder.totalAmount(paymentTotals);
        BigDecimal cashIncome = cashMovementService.sumIncomeByCashRegister(cashRegister.getId());
        BigDecimal cashWithdrawal = cashMovementService.sumWithdrawalByCashRegister(cashRegister.getId());
        List<ShiftCashMovementResponse> movements =
                cashMovementService.listMovementsByCashRegister(cashRegister.getId());
        BigDecimal finalCash = cashMovementService.expectedFinalCashForCashRegister(
                cashRegister.getId(),
                cashRegister.getInitialCash(),
                paymentTotals.cash());

        return new CloseReportResponse(
                "CASH_REGISTER",
                cashRegister.getOpenedAt(),
                cashRegister.getOpenedBy().getName(),
                cashRegister.getInitialCash(),
                closedAt,
                user.getName(),
                null,
                null,
                null,
                null,
                null,
                cashRegister.getInitialCash(),
                finalCash,
                salesCount,
                totalSales,
                paymentTotals,
                movements,
                cashIncome,
                cashWithdrawal);
    }

    private CashRegisterActiveResponse toActiveResponse(CashRegister cashRegister) {
        PaymentTotalsResponse paymentTotals = closeReportBuilder.paymentTotalsByCashRegister(cashRegister.getId());
        BigDecimal cashIncome = cashMovementService.sumIncomeByCashRegister(cashRegister.getId());
        BigDecimal cashWithdrawal = cashMovementService.sumWithdrawalByCashRegister(cashRegister.getId());
        BigDecimal expectedFinalCash = cashMovementService.expectedFinalCashForCashRegister(
                cashRegister.getId(),
                cashRegister.getInitialCash(),
                paymentTotals.cash());
        return new CashRegisterActiveResponse(
                DtoMapper.toCashRegisterResponse(cashRegister),
                cashMovementService.listMovementsByCashRegister(cashRegister.getId()),
                paymentTotals.cash(),
                cashIncome,
                cashWithdrawal,
                expectedFinalCash);
    }

    public CashRegister getOpenCashRegisterForToday() {
        return findOpenCashRegisterForToday()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No hay caja abierta para hoy"));
    }

    @Transactional(readOnly = true)
    public BigDecimal initialCashForNewShift(CashRegister cashRegister) {
        if (!shiftRepository.existsByCashRegisterId(cashRegister.getId())) {
            return cashRegister.getInitialCash();
        }
        PaymentTotalsResponse paymentTotals = closeReportBuilder.paymentTotalsByCashRegister(cashRegister.getId());
        return cashMovementService.expectedFinalCashForCashRegister(
                cashRegister.getId(),
                cashRegister.getInitialCash(),
                paymentTotals.cash());
    }

    private Optional<CashRegister> findOpenCashRegisterForToday() {
        return cashRegisterRepository.findFirstByBusinessDateAndStatusOrderByOpenedAtDesc(
                today(), CashRegisterStatus.OPEN);
    }

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
