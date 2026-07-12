package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.StockAdjustRequest;
import com.pasteleria.pos.dto.StockPurchaseRequest;
import com.pasteleria.pos.dto.StockResponse;
import com.pasteleria.pos.service.StockService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/product/{productId}")
    public StockResponse getCurrentStock(@PathVariable UUID productId) {
        return stockService.getCurrentStock(productId);
    }

    @PostMapping("/adjust")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse adjustStock(@Valid @RequestBody StockAdjustRequest request) {
        return stockService.adjustStock(request);
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse recordPurchase(@Valid @RequestBody StockPurchaseRequest request) {
        return stockService.recordPurchase(request);
    }
}
