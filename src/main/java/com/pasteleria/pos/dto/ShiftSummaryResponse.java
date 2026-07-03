package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ShiftStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftSummaryResponse(
        UUID id,
        UUID cashRegisterId,
        String sellerName,
        ShiftStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        BigDecimal initialCash,
        long salesCount,
        BigDecimal totalSalesAmount,
        UUID companyId,
        String companyName
) {
}
