package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.DiscountType;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        UUID sellerId,
        String sellerName,
        PaymentMethod paymentMethod,
        Integer installments,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        BigDecimal cashAmount,
        DiscountType totalDiscountType,
        BigDecimal totalDiscountValue,
        BigDecimal costTotal,
        BigDecimal profit,
        OffsetDateTime createdAt,
        List<SaleItemResponse> items
) {
}
