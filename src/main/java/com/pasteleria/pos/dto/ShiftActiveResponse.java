package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShiftActiveResponse(
        ShiftResponse shift,
        List<ShiftCashMovementResponse> cashMovements,
        BigDecimal cashSales,
        BigDecimal cashIncome,
        BigDecimal cashWithdrawal,
        BigDecimal expectedFinalCash
) {
}
