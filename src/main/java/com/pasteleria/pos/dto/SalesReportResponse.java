package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SalesReportResponse(
        long totalSalesCount,
        BigDecimal totalAmount,
        Map<PaymentMethod, BigDecimal> amountByPaymentMethod,
        List<SaleResponse> sales
) {
}
