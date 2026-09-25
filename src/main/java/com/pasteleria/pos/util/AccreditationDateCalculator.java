package com.pasteleria.pos.util;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class AccreditationDateCalculator {

    static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final int CARD_BUSINESS_DAYS = 2;
    private static final int PEDIDOSYA_CALENDAR_DAYS = 7;

    private AccreditationDateCalculator() {
    }

    public static LocalDate calculate(PaymentMethod paymentMethod, OffsetDateTime createdAt) {
        LocalDate saleDate = saleDate(createdAt);
        if (paymentMethod == null) {
            return saleDate;
        }
        return switch (paymentMethod) {
            case TARJETA, DEBITO, QR -> addBusinessDays(saleDate, CARD_BUSINESS_DAYS);
            case PEDIDOSYA -> saleDate.plusDays(PEDIDOSYA_CALENDAR_DAYS);
            case EFECTIVO, TRANSFERENCIA -> saleDate;
        };
    }

    private static LocalDate saleDate(OffsetDateTime createdAt) {
        OffsetDateTime instant = createdAt != null ? createdAt : OffsetDateTime.now(ZONE);
        return instant.atZoneSameInstant(ZONE).toLocalDate();
    }

    static LocalDate addBusinessDays(LocalDate start, int businessDays) {
        LocalDate date = start;
        int added = 0;
        while (added < businessDays) {
            date = date.plusDays(1);
            if (isBusinessDay(date)) {
                added++;
            }
        }
        return date;
    }

    private static boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }
}
