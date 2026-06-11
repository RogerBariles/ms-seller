package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.dto.CompanyRequest;
import com.pasteleria.pos.dto.CompanyResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.CompanyRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public List<CompanyResponse> listCompanies() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(DtoMapper::toCompanyResponse)
                .toList();
    }

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(request.name().trim());
        company.setDetail(request.detail());
        company.setActive(request.active());
        return DtoMapper.toCompanyResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse updateCompany(UUID id, CompanyRequest request) {
        Company company = getCompanyEntity(id);
        company.setName(request.name().trim());
        company.setDetail(request.detail());
        company.setActive(request.active());
        return DtoMapper.toCompanyResponse(companyRepository.save(company));
    }

    public Company getCompanyEntity(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    }
}
