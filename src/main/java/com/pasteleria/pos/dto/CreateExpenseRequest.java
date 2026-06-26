package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
    String detail,
    BigDecimal amount,
    LocalDate date
) {}
