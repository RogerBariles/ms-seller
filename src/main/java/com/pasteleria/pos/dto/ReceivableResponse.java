package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableResponse(
        LocalDate date,
        PaymentMethod paymentMethod,
        BigDecimal amount
) {
}
