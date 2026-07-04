package com.pasteleria.pos.controller;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.dto.SalesReportResponse;
import com.pasteleria.pos.dto.TopStatsResponse;
import com.pasteleria.pos.service.ReportService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) UUID companyId) {
        return reportService.getSalesReport(
                fromDate,
                toDate,
                paymentMethod,
                sellerId,
                companyId);
    }

    @GetMapping("/top-stats")
    public TopStatsResponse topStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) UUID companyId) {
        return reportService.getTopStats(
                fromDate,
                toDate,
                paymentMethod,
                sellerId,
                companyId);
    }
}
