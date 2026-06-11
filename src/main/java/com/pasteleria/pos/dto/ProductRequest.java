package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @DecimalMin("0.00") BigDecimal purchasePrice,
        boolean active
) {
}
