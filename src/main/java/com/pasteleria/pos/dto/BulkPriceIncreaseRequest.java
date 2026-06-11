package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.PriceField;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BulkPriceIncreaseRequest(
        @NotNull @DecimalMin("0.01") BigDecimal percentage,
        @NotNull PriceField target
) {
}
