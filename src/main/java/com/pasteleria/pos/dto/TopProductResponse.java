package com.pasteleria.pos.dto;

public record TopProductResponse(
        String productName,
        long totalQuantity
) {}
