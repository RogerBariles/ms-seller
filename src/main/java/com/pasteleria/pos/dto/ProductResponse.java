package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ProductCategory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        ProductCategory category,
        BigDecimal price,
        BigDecimal purchasePrice,
        boolean active,
        OffsetDateTime updatedAt,
        UUID companyId,
        String companyName,
        Integer currentStock
) {
}
