package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.PayableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableResponse(
        UUID id,
        String detail,
        BigDecimal amount,
        LocalDate dueDate,
        PayableStatus status,
        boolean overdue,
        boolean recurring
) {
}
