package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RecurringPayableResponse(
        UUID id,
        String detail,
        BigDecimal amount,
        int dayOfMonth,
        boolean active
) {
}
