package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    String detail,
    BigDecimal amount,
    String createdByName,
    OffsetDateTime createdAt
) {}
