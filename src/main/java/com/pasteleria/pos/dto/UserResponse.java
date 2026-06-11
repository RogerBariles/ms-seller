package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.UserRole;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String username,
        UserRole role,
        boolean active,
        LocalDate birthDate,
        UUID companyId,
        String companyName
) {
}
