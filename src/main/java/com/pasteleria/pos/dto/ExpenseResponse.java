package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ExpensePaymentSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    String detail,
    BigDecimal amount,
    ExpensePaymentSource paymentSource,
    String createdByName,
    OffsetDateTime createdAt
) {}
