package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UserRequest(
        @NotBlank String name,
        @NotBlank
        @Size(min = 3, max = 80)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Usuario inválido")
        String username,
        String password,
        @NotNull UserRole role,
        boolean active,
        LocalDate birthDate,
        UUID companyId
) {
}
