package com.pasteleria.pos.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StockPurchaseRequest(
        @NotNull UUID productId,
        int quantity,
        String notes
) {
}
