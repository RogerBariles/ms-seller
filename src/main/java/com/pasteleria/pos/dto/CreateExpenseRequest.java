package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ExpensePaymentSource;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
    String detail,
    BigDecimal amount,
    LocalDate date,
    ExpensePaymentSource paymentSource
) {}
