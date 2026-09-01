package com.pasteleria.pos.dto;

import java.util.List;
import java.util.UUID;

public record ShiftHoursReportResponse(
        UUID sellerId,
        String sellerName,
        long totalDurationMinutes,
        List<ShiftHoursRowResponse> shifts
) {
}
