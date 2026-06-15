package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ShiftCashMovementRequest(
        @NotNull CashMovementType type,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 500) String detail
) {
}
