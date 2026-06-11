package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.CloseReportResponse;
import com.pasteleria.pos.dto.ShiftResponse;
import com.pasteleria.pos.service.BirthdaySaleScheduler;
import com.pasteleria.pos.service.ShiftService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
public class ShiftController {

    private final ShiftService shiftService;
    private final BirthdaySaleScheduler birthdaySaleScheduler;

    public ShiftController(ShiftService shiftService, BirthdaySaleScheduler birthdaySaleScheduler) {
        this.shiftService = shiftService;
        this.birthdaySaleScheduler = birthdaySaleScheduler;
    }

    @GetMapping("/active")
    public ResponseEntity<ShiftResponse> getActive() {
        return shiftService.getActiveShift()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/start")
    public ShiftResponse start() {
        ShiftResponse shift = shiftService.startShift();
        birthdaySaleScheduler.processBirthdaysForDate(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")));
        return shift;
    }

    @PostMapping("/{id}/close")
    public CloseReportResponse close(@PathVariable UUID id) {
        return shiftService.closeShift(id);
    }
}
