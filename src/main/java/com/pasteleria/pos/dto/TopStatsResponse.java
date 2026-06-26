package com.pasteleria.pos.dto;

import java.util.List;

public record TopStatsResponse(
        List<TopProductResponse> topProducts,
        List<TopDayResponse> topDays,
        List<TopSellerResponse> topSellers
) {}
