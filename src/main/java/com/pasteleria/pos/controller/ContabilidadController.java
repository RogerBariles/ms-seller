package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.ContabilidadResponse;
import com.pasteleria.pos.dto.CreateExpenseRequest;
import com.pasteleria.pos.dto.ExpenseResponse;
import com.pasteleria.pos.service.ContabilidadService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contabilidad")
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
public class ContabilidadController {

    private final ContabilidadService contabilidadService;

    public ContabilidadController(ContabilidadService contabilidadService) {
        this.contabilidadService = contabilidadService;
    }

    @GetMapping("/summary")
    public ContabilidadResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return contabilidadService.getSummary(fromDate, toDate);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@RequestBody CreateExpenseRequest request) {
        return contabilidadService.createExpense(request);
    }
}
