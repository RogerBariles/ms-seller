package com.pasteleria.pos.dto;

import java.math.BigDecimal;

public record CreateExpenseRequest(
    String detail,
    BigDecimal amount
) {}
