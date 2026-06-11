package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer quantity,
        DiscountType discountType,
        @DecimalMin("0.00") BigDecimal discountValue
) {
}
