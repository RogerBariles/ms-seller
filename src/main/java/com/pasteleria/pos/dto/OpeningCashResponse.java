package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Efectivo con el que se abre la próxima caja: el efectivo final de la última caja cerrada.
 * Si la empresa nunca cerró una caja, {@code fromPreviousClose} es false y el monto lo carga el usuario.
 */
public record OpeningCashResponse(
        BigDecimal initialCash,
        boolean fromPreviousClose,
        LocalDate previousBusinessDate,
        OffsetDateTime previousClosedAt,
        String previousClosedByName
) {
}
