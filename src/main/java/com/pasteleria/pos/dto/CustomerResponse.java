package com.pasteleria.pos.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String dni,
        LocalDate birthDate,
        String whatsapp,
        String email,
        /** true si todavía no tiene compras: la venta recibe el descuento de bienvenida. */
        boolean firstPurchase
) {
}
