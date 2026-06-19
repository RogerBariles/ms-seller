package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.dto.CloseReportResponse;
import com.pasteleria.pos.dto.PaymentTotalsResponse;
import com.pasteleria.pos.dto.ShiftCashMovementResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.CashRegisterRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseReportService {

    private final ShiftRepository shiftRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final SaleRepository saleRepository;
    private final CloseReportBuilder closeReportBuilder;
    private final ShiftCashMovementService cashMovementService;

    public CloseReportService(
            ShiftRepository shiftRepository,
            CashRegisterRepository cashRegisterRepository,
            SaleRepository saleRepository,
            CloseReportBuilder closeReportBuilder,
            ShiftCashMovementService cashMovementService) {
        this.shiftRepository = shiftRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.saleRepository = saleRepository;
        this.closeReportBuilder = closeReportBuilder;
        this.cashMovementService = cashMovementService;
    }

    @Transactional(readOnly = true)
    public CloseReportResponse buildShiftReport(UUID shiftId) {
        Shift shift = shiftRepository.findByIdForClose(shiftId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        return buildShiftReport(shift, null);
    }

    @Transactional(readOnly = true)
    public CloseReportResponse buildCashRegisterReport(UUID cashRegisterId) {
        CashRegister cashRegister = cashRegisterRepository.findByIdWithUsers(cashRegisterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caja no encontrada"));
        return buildCashRegisterReport(cashRegister);
    }

    public CloseReportResponse buildShiftReport(Shift shift, String closedByName) {
        CashRegister cashRegister = shift.getCashRegister();
        PaymentTotalsResponse paymentTotals = closeReportBuilder.paymentTotalsByShift(shift.getId());
        long salesCount = saleRepository.countByShiftId(shift.getId());
        BigDecimal totalSales = CloseReportBuilder.totalAmount(paymentTotals);
        BigDecimal cashSales = paymentTotals.cash();
        BigDecimal cashIncome = cashMovementService.sumIncome(shift.getId());
        BigDecimal cashWithdrawal = cashMovementService.sumWithdrawal(shift.getId());
        List<ShiftCashMovementResponse> movements = cashMovementService.listMovements(shift.getId());
        BigDecimal finalCash = cashMovementService.expectedFinalCash(
                shift.getId(), shift.getInitialCash(), cashSales);

        return new CloseReportResponse(
                "SHIFT",
                cashRegister.getOpenedAt(),
                cashRegister.getOpenedBy().getName(),
                cashRegister.getInitialCash(),
                cashRegister.getClosedAt(),
                cashRegister.getClosedBy() != null ? cashRegister.getClosedBy().getName() : null,
                shift.getStartedAt(),
                shift.getSeller().getName(),
                shift.getInitialCash(),
                shift.getEndedAt(),
                closedByName,
                shift.getInitialCash(),
                finalCash,
                salesCount,
                totalSales,
                paymentTotals,
                movements,
                cashIncome,
                cashWithdrawal);
    }

    public CloseReportResponse buildCashRegisterReport(CashRegister cashRegister) {
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
                cashRegister.getClosedAt(),
                cashRegister.getClosedBy() != null ? cashRegister.getClosedBy().getName() : null,
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
}
