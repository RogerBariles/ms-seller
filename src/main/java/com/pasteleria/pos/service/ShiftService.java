package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.dto.CloseReportResponse;
import com.pasteleria.pos.dto.PaymentTotalsResponse;
import com.pasteleria.pos.dto.ShiftActiveResponse;
import com.pasteleria.pos.dto.ShiftCashMovementRequest;
import com.pasteleria.pos.dto.ShiftCashMovementResponse;
import com.pasteleria.pos.dto.ShiftResponse;
import com.pasteleria.pos.dto.ShiftSummaryResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
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
public class ShiftService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ShiftRepository shiftRepository;
    private final CashRegisterService cashRegisterService;
    private final SaleRepository saleRepository;
    private final UserService userService;
    private final CloseReportBuilder closeReportBuilder;
    private final CloseReportService closeReportService;
    private final ShiftCashMovementService cashMovementService;

    public ShiftService(
            ShiftRepository shiftRepository,
            CashRegisterService cashRegisterService,
            SaleRepository saleRepository,
            UserService userService,
            CloseReportBuilder closeReportBuilder,
            CloseReportService closeReportService,
            ShiftCashMovementService cashMovementService) {
        this.shiftRepository = shiftRepository;
        this.cashRegisterService = cashRegisterService;
        this.saleRepository = saleRepository;
        this.userService = userService;
        this.closeReportBuilder = closeReportBuilder;
        this.closeReportService = closeReportService;
        this.cashMovementService = cashMovementService;
    }

    @Transactional(readOnly = true)
    public Optional<ShiftActiveResponse> getActiveShift() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return shiftRepository.findBySellerIdAndStatus(principal.getId(), ShiftStatus.OPEN)
                .map(this::toActiveResponse);
    }

    @Transactional
    public ShiftResponse startShift() {
        UserPrincipal principal = SecurityUtils.currentUser();
        shiftRepository.findBySellerIdAndStatus(principal.getId(), ShiftStatus.OPEN)
                .ifPresent(shift -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Ya tiene un turno abierto");
                });

        CashRegister cashRegister = cashRegisterService.getOpenCashRegisterForToday();
        User seller = userService.getUserEntity(principal.getId());

        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCashRegister(cashRegister);
        shift.setSeller(seller);
        shift.setInitialCash(cashRegisterService.initialCashForNewShift(cashRegister));
        shift.setStatus(ShiftStatus.OPEN);
        shift.setStartedAt(OffsetDateTime.now(ZONE));
        return DtoMapper.toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftCashMovementResponse addCashMovement(UUID shiftId, ShiftCashMovementRequest request) {
        return cashMovementService.addMovement(shiftId, request);
    }

    @Transactional(readOnly = true)
    public List<ShiftSummaryResponse> getShiftsByDate(LocalDate date) {
        return shiftRepository.findByCashRegisterBusinessDateDesc(date).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CloseReportResponse getShiftReport(UUID id) {
        return closeReportService.buildShiftReport(id);
    }

    @Transactional
    public CloseReportResponse closeShift(UUID id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        Shift shift = shiftRepository.findByIdForClose(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        if (!shift.getSeller().getId().equals(principal.getId())
                && principal.getRole() != com.pasteleria.pos.domain.enums.UserRole.SUPER_ADMIN
                && principal.getRole() != com.pasteleria.pos.domain.enums.UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puede cerrar este turno");
        }
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "El turno ya está cerrado");
        }

        User closedBy = userService.getUserEntity(principal.getId());
        OffsetDateTime closedAt = OffsetDateTime.now(ZONE);
        PaymentTotalsResponse paymentTotals = closeReportBuilder.paymentTotalsByShift(shift.getId());
        long salesCount = saleRepository.countByShiftId(shift.getId());

        shift.setCashSalesTotal(paymentTotals.cash());
        shift.setSalesCount((int) salesCount);
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setEndedAt(closedAt);
        shiftRepository.save(shift);

        return closeReportService.buildShiftReport(shift, closedBy.getName());
    }

    private ShiftSummaryResponse toSummary(Shift shift) {
        PaymentTotalsResponse paymentTotals = closeReportBuilder.paymentTotalsByShift(shift.getId());
        return new ShiftSummaryResponse(
                shift.getId(),
                shift.getCashRegister().getId(),
                shift.getSeller().getName(),
                shift.getStatus(),
                shift.getStartedAt(),
                shift.getEndedAt(),
                shift.getInitialCash(),
                saleRepository.countByShiftId(shift.getId()),
                CloseReportBuilder.totalAmount(paymentTotals));
    }

    public Shift getRequiredActiveShiftForSeller(UUID sellerId) {
        return shiftRepository.findBySellerIdAndStatus(sellerId, ShiftStatus.OPEN)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Debe tener caja y turno abiertos para vender"));
    }

    private ShiftActiveResponse toActiveResponse(Shift shift) {
        BigDecimal cashSales = saleRepository.sumTotalByShiftAndPaymentMethod(
                shift.getId(), PaymentMethod.EFECTIVO);
        BigDecimal cashIncome = cashMovementService.sumIncome(shift.getId());
        BigDecimal cashWithdrawal = cashMovementService.sumWithdrawal(shift.getId());
        BigDecimal expectedFinalCash = cashMovementService.expectedFinalCash(
                shift.getId(), shift.getInitialCash(), cashSales);
        return new ShiftActiveResponse(
                DtoMapper.toShiftResponse(shift),
                cashMovementService.listMovements(shift.getId()),
                cashSales,
                cashIncome,
                cashWithdrawal,
                expectedFinalCash);
    }
}
