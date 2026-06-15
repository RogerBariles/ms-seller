package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CloseReportResponse(
        String type,
        OffsetDateTime cashRegisterOpenedAt,
        String cashRegisterOpenedByName,
        BigDecimal cashRegisterInitialCash,
        OffsetDateTime cashRegisterClosedAt,
        String cashRegisterClosedByName,
        OffsetDateTime shiftStartedAt,
        String shiftOpenedByName,
        BigDecimal shiftInitialCash,
        OffsetDateTime shiftClosedAt,
        String shiftClosedByName,
        BigDecimal initialCash,
        BigDecimal finalCash,
        long salesCount,
        BigDecimal totalSalesAmount,
        PaymentTotalsResponse paymentTotals,
        List<ShiftCashMovementResponse> cashMovements,
        BigDecimal cashIncome,
        BigDecimal cashWithdrawal
) {
}
