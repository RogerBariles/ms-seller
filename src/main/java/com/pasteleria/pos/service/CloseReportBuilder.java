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
                sumByShift(shiftId, PaymentMethod.EFECTIVO),
                sumByShift(shiftId, PaymentMethod.TARJETA),
                sumByShift(shiftId, PaymentMethod.TRANSFERENCIA));
    }

    public PaymentTotalsResponse paymentTotalsByCashRegister(UUID cashRegisterId) {
        return new PaymentTotalsResponse(
                sumByCashRegister(cashRegisterId, PaymentMethod.EFECTIVO),
                sumByCashRegister(cashRegisterId, PaymentMethod.TARJETA),
                sumByCashRegister(cashRegisterId, PaymentMethod.TRANSFERENCIA));
    }

    public static BigDecimal totalAmount(PaymentTotalsResponse totals) {
        return totals.cash().add(totals.card()).add(totals.transfer());
    }

    private BigDecimal sumByShift(UUID shiftId, PaymentMethod method) {
        return saleRepository.sumTotalByShiftAndPaymentMethod(shiftId, method);
    }

    private BigDecimal sumByCashRegister(UUID cashRegisterId, PaymentMethod method) {
        return saleRepository.sumTotalByCashRegisterAndPaymentMethod(cashRegisterId, method);
    }
}
