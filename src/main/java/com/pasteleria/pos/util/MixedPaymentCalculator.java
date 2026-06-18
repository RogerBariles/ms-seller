package com.pasteleria.pos.util;

import com.pasteleria.pos.domain.enums.DiscountType;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;

public final class MixedPaymentCalculator {

    private MixedPaymentCalculator() {
    }

    public record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal total) {
    }

    public static Totals calculateTotals(
            BigDecimal subtotal,
            BigDecimal lineDiscountTotal,
            DiscountType totalDiscountType,
            BigDecimal totalDiscountValue,
            BigDecimal manualTotal,
            BigDecimal cashPortion) {
        BigDecimal afterLineDiscounts = subtotal.subtract(lineDiscountTotal);
        BigDecimal safeCashPortion = cashPortion != null ? cashPortion : BigDecimal.ZERO;

        if (manualTotal != null) {
            if (manualTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El total manual debe ser mayor a cero");
            }
            BigDecimal total = manualTotal.setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountTotal = subtotal.subtract(total).setScale(2, RoundingMode.HALF_UP);
            return new Totals(
                    subtotal.setScale(2, RoundingMode.HALF_UP),
                    discountTotal,
                    total);
        }

        BigDecimal discountBase = afterLineDiscounts.subtract(safeCashPortion).max(BigDecimal.ZERO);
        BigDecimal totalDiscount = DiscountCalculator.applyDiscount(
                discountBase, totalDiscountType, totalDiscountValue);
        BigDecimal remainder = discountBase.subtract(totalDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = safeCashPortion.add(remainder).setScale(2, RoundingMode.HALF_UP);

        return new Totals(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                lineDiscountTotal.add(totalDiscount).setScale(2, RoundingMode.HALF_UP),
                total);
    }

    public static BigDecimal resolvePartialCash(
            PaymentMethod paymentMethod, BigDecimal cashAmount, BigDecimal afterLineDiscounts) {
        if (!allowsPartialCash(paymentMethod)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (cashAmount == null || cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (cashAmount.compareTo(afterLineDiscounts) > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El efectivo no puede superar el subtotal después de descuentos por ítem");
        }
        return cashAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal resolveCashAmount(
            PaymentMethod paymentMethod, BigDecimal cashPortion, BigDecimal total) {
        if (paymentMethod == PaymentMethod.EFECTIVO) {
            return total;
        }
        return cashPortion != null ? cashPortion : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean allowsPartialCash(PaymentMethod paymentMethod) {
        return paymentMethod != PaymentMethod.EFECTIVO && paymentMethod != PaymentMethod.PEDIDOSYA;
    }

    public static BigDecimal afterLineDiscounts(BigDecimal subtotal, BigDecimal lineDiscountTotal) {
        return subtotal.subtract(lineDiscountTotal).setScale(2, RoundingMode.HALF_UP);
    }
}
