package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.dto.PaymentTotalsResponse;
import com.pasteleria.pos.repository.SaleRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CloseReportBuilder {

    private final SaleRepository saleRepository;

    public CloseReportBuilder(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public PaymentTotalsResponse paymentTotalsByShift(UUID shiftId) {
        return buildTotals(shiftId, true);
    }

    public PaymentTotalsResponse paymentTotalsByCashRegister(UUID cashRegisterId) {
        return buildTotals(cashRegisterId, false);
    }

    public static BigDecimal totalAmount(PaymentTotalsResponse totals) {
        return totals.cash()
                .add(totals.card())
                .add(totals.transfer())
                .add(totals.pedidosYa())
                .add(totals.debito())
                .add(totals.qr());
    }

    private PaymentTotalsResponse buildTotals(UUID id, boolean byShift) {
        return new PaymentTotalsResponse(
                sum(id, byShift, PaymentMethod.EFECTIVO),
                sum(id, byShift, PaymentMethod.TARJETA),
                sum(id, byShift, PaymentMethod.TRANSFERENCIA),
                sum(id, byShift, PaymentMethod.PEDIDOSYA),
                sum(id, byShift, PaymentMethod.DEBITO),
                sum(id, byShift, PaymentMethod.QR));
    }

    private BigDecimal sum(UUID id, boolean byShift, PaymentMethod method) {
        return byShift
                ? saleRepository.sumTotalByShiftAndPaymentMethod(id, method)
                : saleRepository.sumTotalByCashRegisterAndPaymentMethod(id, method);
    }
}
