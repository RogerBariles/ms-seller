package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.PriceChangeType;
import com.pasteleria.pos.domain.enums.PriceField;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductPriceAuditResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID changedById,
        String changedByName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        PriceField priceField,
        PriceChangeType changeType,
        OffsetDateTime changedAt
) {
}
