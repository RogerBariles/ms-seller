package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.CustomerListItemResponse;
import com.pasteleria.pos.dto.CustomerRequest;
import com.pasteleria.pos.dto.CustomerResponse;
import com.pasteleria.pos.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** Sección Clientes: todos los clientes de todas las empresas. */
    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN', 'DEVELOPER')")
    public List<CustomerListItemResponse> list() {
        return customerService.listAll();
    }

    /** 200 con el cliente, o 204 si no existe. */
    @GetMapping("/by-dni")
    public ResponseEntity<CustomerResponse> findByDni(@RequestParam String dni) {
        return customerService.findByDni(dni)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }
}
