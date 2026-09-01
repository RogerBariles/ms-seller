package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.dto.ShiftHoursReportResponse;
import com.pasteleria.pos.dto.ShiftHoursRowResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.repository.UserRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftReportService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public ShiftReportService(ShiftRepository shiftRepository, UserRepository userRepository) {
        this(shiftRepository, userRepository, Clock.system(ZONE));
    }

    ShiftReportService(ShiftRepository shiftRepository, UserRepository userRepository, Clock clock) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ShiftHoursReportResponse getShiftHoursReport(LocalDate fromDate, LocalDate toDate, UUID sellerId) {
        if (fromDate == null || toDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar rango de fechas");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }
        if (sellerId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar una vendedora");
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        UUID companyId = principal.getCompanyId();
        if (companyId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario debe pertenecer a una empresa");
        }

        User seller = userRepository.findByIdWithCompany(sellerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vendedora no encontrada"));
        if (seller.getCompany() == null || !companyId.equals(seller.getCompany().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La vendedora no pertenece a la empresa");
        }

        OffsetDateTime from = OffsetDateTime.of(fromDate, LocalTime.MIN, ZONE.getRules().getOffset(fromDate.atStartOfDay()));
        OffsetDateTime to = OffsetDateTime.of(toDate, LocalTime.MAX, ZONE.getRules().getOffset(toDate.atStartOfDay()));
        OffsetDateTime now = OffsetDateTime.now(clock);

        List<Shift> shifts = shiftRepository.findBySellerAndCompanyAndStartedAtBetween(
                sellerId, companyId, from, to);

        List<ShiftHoursRowResponse> rows = shifts.stream()
                .map(shift -> toRow(shift, now))
                .toList();
        long totalMinutes = rows.stream().mapToLong(ShiftHoursRowResponse::durationMinutes).sum();

        return new ShiftHoursReportResponse(seller.getId(), seller.getName(), totalMinutes, rows);
    }

    private static ShiftHoursRowResponse toRow(Shift shift, OffsetDateTime now) {
        OffsetDateTime endedAt = shift.getEndedAt();
        OffsetDateTime end = endedAt != null ? endedAt : now;
        long minutes = Math.max(0, Duration.between(shift.getStartedAt(), end).toMinutes());
        return new ShiftHoursRowResponse(
                shift.getId(),
                shift.getStartedAt(),
                endedAt,
                shift.getStatus(),
                minutes);
    }
}
