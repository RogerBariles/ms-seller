package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.PriceField;
import com.pasteleria.pos.domain.enums.ProductCategory;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BulkPriceIncreaseRequest(
        @NotNull BigDecimal percentage,
        @NotNull PriceField target,
        ProductCategory category
) {
}
