package com.pasteleria.pos.dto;

import java.math.BigDecimal;

public record TopSellerResponse(
        String sellerName,
        long totalSales,
        BigDecimal totalAmount
) {}
