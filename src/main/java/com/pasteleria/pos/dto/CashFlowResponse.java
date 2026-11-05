package com.pasteleria.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowResponse(
        LocalDate fromDate,
        LocalDate toDate,
        List<ReceivableResponse> receivables,
        BigDecimal totalReceivable,
        List<PayableResponse> payables,
        BigDecimal totalPayable,
        BigDecimal totalOverdue,
        BigDecimal balance
) {
}
