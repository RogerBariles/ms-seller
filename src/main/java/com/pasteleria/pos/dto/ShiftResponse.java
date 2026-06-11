package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ShiftStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        UUID cashRegisterId,
        UUID sellerId,
        String sellerName,
        BigDecimal initialCash,
        ShiftStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {
}
