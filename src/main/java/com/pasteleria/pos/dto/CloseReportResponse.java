package com.pasteleria.pos.dto;

import java.math.BigDecimal;

public record CloseReportResponse(
        BigDecimal initialCash,
        BigDecimal finalCash,
        long salesCount,
        BigDecimal totalSalesAmount
) {
}
