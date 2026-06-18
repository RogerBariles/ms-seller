package com.pasteleria.pos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pasteleria.pos.domain.enums.DiscountType;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.exception.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MixedPaymentCalculatorTest {

    @Test
    void mixedTransferWithCashAndPercentageDiscount() {
        MixedPaymentCalculator.Totals totals = MixedPaymentCalculator.calculateTotals(
                bd("10000.00"),
                bd("0.00"),
                DiscountType.PERCENTAGE,
                bd("5"),
                null,
                bd("5000.00"));

        assertEquals(bd("10000.00"), totals.subtotal());
        assertEquals(bd("250.00"), totals.discountTotal());
        assertEquals(bd("9750.00"), totals.total());
    }

    @Test
    void cashPaymentStoresFullTotalAsCashAmount() {
        BigDecimal cashPortion = MixedPaymentCalculator.resolvePartialCash(
                PaymentMethod.TRANSFERENCIA, bd("5000.00"), bd("10000.00"));
        MixedPaymentCalculator.Totals totals = MixedPaymentCalculator.calculateTotals(
                bd("10000.00"),
                bd("0.00"),
                DiscountType.PERCENTAGE,
                bd("5"),
                null,
                cashPortion);
        BigDecimal cashAmount = MixedPaymentCalculator.resolveCashAmount(
                PaymentMethod.TRANSFERENCIA, cashPortion, totals.total());

        assertEquals(bd("5000.00"), cashAmount);
        assertEquals(bd("4750.00"), totals.total().subtract(cashAmount));
    }

    @Test
    void pureCashPaymentUsesFullTotal() {
        MixedPaymentCalculator.Totals totals = MixedPaymentCalculator.calculateTotals(
                bd("10000.00"),
                bd("0.00"),
                DiscountType.PERCENTAGE,
                bd("5"),
                null,
                bd("0.00"));
        BigDecimal cashAmount = MixedPaymentCalculator.resolveCashAmount(
                PaymentMethod.EFECTIVO, bd("0.00"), totals.total());

        assertEquals(bd("9500.00"), totals.total());
        assertEquals(bd("9500.00"), cashAmount);
    }

    @Test
    void rejectsCashAboveAfterLineDiscounts() {
        assertThrows(ApiException.class, () -> MixedPaymentCalculator.resolvePartialCash(
                PaymentMethod.TARJETA, bd("6000.00"), bd("5000.00")));
    }

    @Test
    void pedidosYaDoesNotAllowPartialCash() {
        assertEquals(
                bd("0.00"),
                MixedPaymentCalculator.resolvePartialCash(
                        PaymentMethod.PEDIDOSYA, bd("5000.00"), bd("10000.00")));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
