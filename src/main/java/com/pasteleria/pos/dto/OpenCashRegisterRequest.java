package com.pasteleria.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * El efectivo inicial solo se usa cuando la empresa no tiene ninguna caja cerrada anterior;
 * en cualquier otro caso se toma automáticamente el efectivo final de la última caja cerrada.
 */
public record OpenCashRegisterRequest(
        @DecimalMin("0.00") BigDecimal initialCash
) {
}
