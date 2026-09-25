package com.pasteleria.pos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AccreditationDateCalculatorTest {

    @Test
    void cashAndTransferCreditOnTheSaleDate() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-25T15:30:00-03:00");

        assertEquals(LocalDate.of(2026, 9, 25),
                AccreditationDateCalculator.calculate(PaymentMethod.EFECTIVO, createdAt));
        assertEquals(LocalDate.of(2026, 9, 25),
                AccreditationDateCalculator.calculate(PaymentMethod.TRANSFERENCIA, createdAt));
    }

    @Test
    void cardDebitAndQrCreditTwoBusinessDaysLater() {
        OffsetDateTime friday = OffsetDateTime.parse("2026-09-25T18:00:00-03:00");
        OffsetDateTime thursday = OffsetDateTime.parse("2026-09-24T18:00:00-03:00");
        OffsetDateTime saturday = OffsetDateTime.parse("2026-09-26T12:00:00-03:00");

        assertEquals(LocalDate.of(2026, 9, 29),
                AccreditationDateCalculator.calculate(PaymentMethod.TARJETA, friday));
        assertEquals(LocalDate.of(2026, 9, 28),
                AccreditationDateCalculator.calculate(PaymentMethod.DEBITO, thursday));
        assertEquals(LocalDate.of(2026, 9, 29),
                AccreditationDateCalculator.calculate(PaymentMethod.QR, saturday));
    }

    @Test
    void pedidosYaCreditsOneCalendarWeekLater() {
        OffsetDateTime monday = OffsetDateTime.parse("2026-09-21T11:00:00-03:00");

        assertEquals(LocalDate.of(2026, 9, 28),
                AccreditationDateCalculator.calculate(PaymentMethod.PEDIDOSYA, monday));
    }

    @Test
    void usesArgentinaCalendarDateWhenInstantIsAlreadyTheNextUtcDay() {
        OffsetDateTime lateEveningInArgentina = OffsetDateTime.of(2026, 9, 26, 2, 30, 0, 0, ZoneOffset.UTC);

        assertEquals(LocalDate.of(2026, 9, 25),
                AccreditationDateCalculator.calculate(PaymentMethod.EFECTIVO, lateEveningInArgentina));
    }
}
