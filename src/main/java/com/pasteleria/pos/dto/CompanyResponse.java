package com.pasteleria.pos.dto;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String detail,
        boolean active
) {
}
