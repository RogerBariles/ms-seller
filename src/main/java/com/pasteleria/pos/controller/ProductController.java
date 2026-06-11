package com.pasteleria.pos.controller;

import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.dto.BulkPriceIncreaseRequest;
import com.pasteleria.pos.dto.ProductPriceAuditResponse;
import com.pasteleria.pos.dto.ProductRequest;
import com.pasteleria.pos.dto.ProductResponse;
import com.pasteleria.pos.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listProducts();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category) {
        return productService.search(q, category);
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/bulk-price-increase")
    public Map<String, Integer> bulkPriceIncrease(@Valid @RequestBody BulkPriceIncreaseRequest request) {
        int updated = productService.bulkPriceIncrease(request);
        return Map.of("updated", updated);
    }

    @GetMapping("/{id}/audits")
    public List<ProductPriceAuditResponse> audits(@PathVariable UUID id) {
        return productService.getProductAudit(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }
}
