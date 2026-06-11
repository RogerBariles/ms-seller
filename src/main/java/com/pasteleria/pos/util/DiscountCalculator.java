package com.pasteleria.pos.util;

import com.pasteleria.pos.domain.enums.DiscountType;
import com.pasteleria.pos.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;

public final class DiscountCalculator {

    private static final int SCALE = 2;

    private DiscountCalculator() {
    }

    public static BigDecimal applyDiscount(BigDecimal amount, DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return switch (type) {
            case PERCENTAGE -> amount.multiply(value)
                    .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
            case FIXED -> value.min(amount).setScale(SCALE, RoundingMode.HALF_UP);
        };
    }

    public static void validateDiscount(DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El descuento no puede ser negativo");
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El porcentaje no puede superar 100");
        }
    }
}
