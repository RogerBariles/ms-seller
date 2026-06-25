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
                cashTotal(id, byShift),
                nonCashTotal(id, byShift, PaymentMethod.TARJETA),
                nonCashTotal(id, byShift, PaymentMethod.TRANSFERENCIA),
                nonCashTotal(id, byShift, PaymentMethod.PEDIDOSYA),
                nonCashTotal(id, byShift, PaymentMethod.DEBITO),
                nonCashTotal(id, byShift, PaymentMethod.QR));
    }

    private BigDecimal cashTotal(UUID id, boolean byShift) {
        return byShift
                ? saleRepository.sumCashAmountByShift(id)
                : saleRepository.sumCashAmountByCashRegister(id);
    }

    private BigDecimal nonCashTotal(UUID id, boolean byShift, PaymentMethod method) {
        return byShift
                ? saleRepository.sumNonCashAmountByShiftAndPaymentMethod(id, method)
                : saleRepository.sumNonCashAmountByCashRegisterAndPaymentMethod(id, method);
    }
}
