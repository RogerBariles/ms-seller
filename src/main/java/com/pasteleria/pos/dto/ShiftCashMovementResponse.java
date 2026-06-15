package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.CashMovementType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftCashMovementResponse(
        UUID id,
        CashMovementType type,
        BigDecimal amount,
        String detail,
        String createdByName,
        OffsetDateTime createdAt
) {
}
