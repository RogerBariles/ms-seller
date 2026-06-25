package com.pasteleria.pos.dto;

import java.math.BigDecimal;

public record PaymentTotalsResponse(
        BigDecimal cash,
        BigDecimal card,
        BigDecimal transfer,
        BigDecimal pedidosYa,
        BigDecimal debito,
        BigDecimal qr
) {
}
