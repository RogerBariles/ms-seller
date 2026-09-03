package com.pasteleria.pos.controller;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.dto.SalesReportResponse;
import com.pasteleria.pos.dto.ShiftHoursReportResponse;
import com.pasteleria.pos.dto.TopStatsResponse;
import com.pasteleria.pos.service.ReportService;
import com.pasteleria.pos.service.ShiftReportService;
import java.time.LocalDate;
import java.util.List;
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
    private final ShiftReportService shiftReportService;

    public ReportController(ReportService reportService, ShiftReportService shiftReportService) {
        this.reportService = reportService;
        this.shiftReportService = shiftReportService;
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) List<PaymentMethod> paymentMethod,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) List<ProductCategory> category) {
        return reportService.getSalesReport(
                fromDate,
                toDate,
                paymentMethod,
                sellerId,
                companyId,
                category);
    }

    @GetMapping("/top-stats")
    public TopStatsResponse topStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) List<PaymentMethod> paymentMethod,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) List<ProductCategory> category) {
        return reportService.getTopStats(
                fromDate,
                toDate,
                paymentMethod,
                sellerId,
                companyId,
                category);
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ShiftHoursReportResponse shifts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam UUID sellerId) {
        return shiftReportService.getShiftHoursReport(fromDate, toDate, sellerId);
    }
}
