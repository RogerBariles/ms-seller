package com.pasteleria.pos.dto;

import java.util.UUID;

public record StockResponse(
        UUID productId,
        String productName,
        int currentStock
) {
}
