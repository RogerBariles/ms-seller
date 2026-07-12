package com.pasteleria.pos.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StockAdjustRequest(
        @NotNull UUID productId,
        int quantityChange,
        String notes
) {
}
