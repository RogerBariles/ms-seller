package com.pasteleria.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RecurringPayableRequest(
        @NotBlank @Size(max = 500) String detail,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Min(1) @Max(31) int dayOfMonth,
        boolean active
) {
}
