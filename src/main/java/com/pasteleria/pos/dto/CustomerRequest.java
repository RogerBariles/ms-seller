package com.pasteleria.pos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** WhatsApp o mail: al menos uno (se valida en el servicio). */
public record CustomerRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank
        @Pattern(regexp = "^\\d{8,15}$", message = "El DNI debe tener al menos 8 dígitos (solo números)")
        String dni,
        @NotNull @Past LocalDate birthDate,
        @Size(max = 30) String whatsapp,
        @Email @Size(max = 180) String email
) {
}
