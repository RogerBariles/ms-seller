package com.pasteleria.pos.dto;

import java.time.LocalDate;

public record TopDayResponse(
        LocalDate date,
        long totalQuantity
) {}
