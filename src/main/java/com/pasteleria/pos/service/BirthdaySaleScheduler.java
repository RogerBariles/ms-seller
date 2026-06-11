package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BirthdaySaleScheduler {

    private static final Logger log = LoggerFactory.getLogger(BirthdaySaleScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final ShiftRepository shiftRepository;
    private final CashRegisterService cashRegisterService;
    private final SaleService saleService;

    public BirthdaySaleScheduler(
            UserRepository userRepository,
            SaleRepository saleRepository,
            ShiftRepository shiftRepository,
            CashRegisterService cashRegisterService,
            SaleService saleService) {
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
        this.shiftRepository = shiftRepository;
        this.cashRegisterService = cashRegisterService;
        this.saleService = saleService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public void processBirthdays() {
        processBirthdaysForDate(LocalDate.now(ZONE));
    }

    @Transactional
    public void processBirthdaysForDate(LocalDate date) {
        try {
            cashRegisterService.getOpenCashRegisterForToday();
        } catch (Exception ex) {
            log.info("Sin caja abierta, se omiten ventas de cumpleaños");
            return;
        }

        Shift shift = shiftRepository.findFirstByStatusOrderByStartedAtAsc(ShiftStatus.OPEN).orElse(null);
        if (shift == null) {
            log.info("Sin turno abierto, se omiten ventas de cumpleaños");
            return;
        }

        OffsetDateTime dayStart = date.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();

        for (User user : userRepository.findActiveWithBirthdayOn(date.getMonthValue(), date.getDayOfMonth())) {
            if (saleRepository.existsBySellerIdAndBirthdayTrueAndCreatedAtBetween(
                    user.getId(), dayStart, dayEnd)) {
                continue;
            }
            saleService.createBirthdaySale(user, shift);
            log.info("Venta de cumpleaños creada para {}", user.getName());
        }
    }
}
