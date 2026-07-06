package com.pasteleria.pos.controller;

import com.pasteleria.pos.dto.CompanyRequest;
import com.pasteleria.pos.dto.CompanyResponse;
import com.pasteleria.pos.service.CompanyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEVELOPER')")
    public List<CompanyResponse> list() {
        return companyService.listCompanies();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEVELOPER')")
    public CompanyResponse create(@Valid @RequestBody CompanyRequest request) {
        return companyService.createCompany(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEVELOPER')")
    public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request) {
        return companyService.updateCompany(id, request);
    }
}
