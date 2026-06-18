package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.DiscountType;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateSaleRequest(
        @NotEmpty @Valid List<SaleItemRequest> items,
        @NotNull PaymentMethod paymentMethod,
        @Min(1) Integer installments,
        DiscountType totalDiscountType,
        @DecimalMin("0.00") BigDecimal totalDiscountValue,
        @DecimalMin("0.01") BigDecimal manualTotal,
        @DecimalMin("0.00") BigDecimal cashAmount
) {
}
