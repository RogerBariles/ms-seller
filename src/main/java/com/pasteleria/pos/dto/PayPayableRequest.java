package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ExpensePaymentSource;
import jakarta.validation.constraints.NotNull;

public record PayPayableRequest(
        @NotNull ExpensePaymentSource paymentSource
) {
}
