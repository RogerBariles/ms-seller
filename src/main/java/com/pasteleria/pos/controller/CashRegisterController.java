package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.CashRegisterActiveResponse;
import com.pasteleria.pos.dto.CashRegisterResponse;
import com.pasteleria.pos.dto.CloseReportResponse;
import com.pasteleria.pos.dto.OpenCashRegisterRequest;
import com.pasteleria.pos.service.CashRegisterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-registers")
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    public CashRegisterController(CashRegisterService cashRegisterService) {
        this.cashRegisterService = cashRegisterService;
    }

    @GetMapping("/today")
    public ResponseEntity<CashRegisterActiveResponse> getToday() {
        return cashRegisterService.getTodayCashRegister()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/today/history")
    public List<CashRegisterResponse> getTodayHistory() {
        return cashRegisterService.getTodayCashRegisterHistory();
    }

    @PostMapping("/open")
    public CashRegisterResponse open(@Valid @RequestBody OpenCashRegisterRequest request) {
        return cashRegisterService.openCashRegister(request);
    }

    @PostMapping("/{id}/close")
    public CloseReportResponse close(@PathVariable UUID id) {
        return cashRegisterService.closeCashRegister(id);
    }
}
