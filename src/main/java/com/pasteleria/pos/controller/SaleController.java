package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.CreateSaleRequest;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.service.SaleService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public SaleResponse create(@Valid @RequestBody CreateSaleRequest request) {
        return saleService.createSale(request);
    }

    @GetMapping("/{id}")
    public SaleResponse getById(@PathVariable UUID id) {
        return saleService.getSale(id);
    }
}
