package com.pasteleria.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Efectivo que queda en la caja al cerrarla; es el inicial de la próxima caja. */
public record CloseCashRegisterRequest(
        @NotNull @DecimalMin("0.00") BigDecimal closingCash
) {
}
