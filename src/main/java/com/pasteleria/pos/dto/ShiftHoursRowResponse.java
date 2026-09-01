package com.pasteleria.pos.dto;

import com.pasteleria.pos.domain.enums.ShiftStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftHoursRowResponse(
        UUID id,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        ShiftStatus status,
        long durationMinutes
) {
}
