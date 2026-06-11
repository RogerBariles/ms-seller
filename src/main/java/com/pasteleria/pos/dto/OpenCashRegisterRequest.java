package com.pasteleria.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpenCashRegisterRequest(
        @NotNull @DecimalMin("0.00") BigDecimal initialCash
) {
}
