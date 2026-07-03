package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashRegisterSummaryResponse(
        UUID id,
        LocalDate businessDate,
        CashRegisterStatus status,
        String openedByName,
        String closedByName,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        BigDecimal initialCash,
        long salesCount,
        BigDecimal totalSalesAmount,
        UUID companyId,
        String companyName
) {
}
