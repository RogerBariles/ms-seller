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
        return new PaymentTotalsResponse(
                saleRepository.sumCashAmountByShift(shiftId),
                sumNonCashByShift(shiftId, PaymentMethod.TARJETA),
                sumNonCashByShift(shiftId, PaymentMethod.TRANSFERENCIA));
    }

    public PaymentTotalsResponse paymentTotalsByCashRegister(UUID cashRegisterId) {
        return new PaymentTotalsResponse(
                saleRepository.sumCashAmountByCashRegister(cashRegisterId),
                sumNonCashByCashRegister(cashRegisterId, PaymentMethod.TARJETA),
                sumNonCashByCashRegister(cashRegisterId, PaymentMethod.TRANSFERENCIA));
    }

    public static BigDecimal totalAmount(PaymentTotalsResponse totals) {
        return totals.cash().add(totals.card()).add(totals.transfer());
    }

    private BigDecimal sumNonCashByShift(UUID shiftId, PaymentMethod method) {
        return saleRepository.sumNonCashAmountByShiftAndPaymentMethod(shiftId, method);
    }

    private BigDecimal sumNonCashByCashRegister(UUID cashRegisterId, PaymentMethod method) {
        return saleRepository.sumNonCashAmountByCashRegisterAndPaymentMethod(cashRegisterId, method);
    }
}
