package com.pasteleria.pos.controller;

import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.dto.SalesReportResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.service.ReportService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String fromTime,
            @RequestParam(required = false) String toTime,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) UUID companyId) {
        return reportService.getSalesReport(
                fromDate,
                toDate,
                parseTime(fromTime),
                parseTime(toTime),
                paymentMethod,
                sellerId,
                companyId);
    }

    private static LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.length() == 5 ? value + ":00" : value;
            return LocalTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Formato de hora inválido");
        }
    }
}
