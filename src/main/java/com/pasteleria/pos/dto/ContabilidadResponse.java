package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ContabilidadResponse(
    BigDecimal totalSales,
    BigDecimal totalExpenses,
    BigDecimal netAmount,
    List<ExpenseResponse> expenses
) {}
