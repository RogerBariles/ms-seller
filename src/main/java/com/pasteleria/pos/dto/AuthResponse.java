package com.pasteleria.pos.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
