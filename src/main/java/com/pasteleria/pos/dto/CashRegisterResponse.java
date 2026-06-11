package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashRegisterResponse(
        UUID id,
        LocalDate businessDate,
        BigDecimal initialCash,
        CashRegisterStatus status,
        UUID openedById,
        String openedByName,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt
) {
}
