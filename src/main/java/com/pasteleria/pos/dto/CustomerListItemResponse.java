package com.pasteleria.pos.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Fila de la sección Clientes (todas las empresas ven todos los clientes). */
public record CustomerListItemResponse(
        UUID id,
        String fullName,
        String dni,
        LocalDate birthDate,
        String whatsapp,
        String email,
        String companyName,
        String createdByName,
        OffsetDateTime createdAt
) {
}
