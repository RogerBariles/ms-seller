package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.DiscountType;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResponse(
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal unitPurchasePrice,
        BigDecimal unitRealPrice,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal lineSubtotal,
        BigDecimal lineDiscount,
        BigDecimal lineTotal,
        BigDecimal lineCost,
        BigDecimal lineProfit
) {
}
