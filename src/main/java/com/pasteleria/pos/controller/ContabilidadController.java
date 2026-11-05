package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.CashFlowResponse;
import com.pasteleria.pos.dto.ContabilidadResponse;
import com.pasteleria.pos.dto.CreateExpenseRequest;
import com.pasteleria.pos.dto.ExpenseResponse;
import com.pasteleria.pos.dto.PayPayableRequest;
import com.pasteleria.pos.dto.PayableRequest;
import com.pasteleria.pos.dto.PayableResponse;
import com.pasteleria.pos.dto.RecurringPayableRequest;
import com.pasteleria.pos.dto.RecurringPayableResponse;
import com.pasteleria.pos.service.CashFlowService;
import com.pasteleria.pos.service.ContabilidadService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final CashFlowService cashFlowService;

    public ContabilidadController(ContabilidadService contabilidadService, CashFlowService cashFlowService) {
        this.contabilidadService = contabilidadService;
        this.cashFlowService = cashFlowService;
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

    @GetMapping("/cash-flow")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CashFlowResponse cashFlow(@RequestParam(defaultValue = "30") int days) {
        return cashFlowService.getCashFlow(days);
    }

    @PostMapping("/payables")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PayableResponse createPayable(@Valid @RequestBody PayableRequest request) {
        return cashFlowService.createPayable(request);
    }

    @PostMapping("/payables/{id}/pay")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PayableResponse payPayable(@PathVariable UUID id, @Valid @RequestBody PayPayableRequest request) {
        return cashFlowService.payPayable(id, request);
    }

    @PostMapping("/payables/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PayableResponse cancelPayable(@PathVariable UUID id) {
        return cashFlowService.cancelPayable(id);
    }

    @GetMapping("/recurring-payables")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<RecurringPayableResponse> recurringPayables() {
        return cashFlowService.listRecurring();
    }

    @PostMapping("/recurring-payables")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RecurringPayableResponse createRecurringPayable(@Valid @RequestBody RecurringPayableRequest request) {
        return cashFlowService.createRecurring(request);
    }

    @PutMapping("/recurring-payables/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RecurringPayableResponse updateRecurringPayable(
            @PathVariable UUID id,
            @Valid @RequestBody RecurringPayableRequest request) {
        return cashFlowService.updateRecurring(id, request);
    }
}
