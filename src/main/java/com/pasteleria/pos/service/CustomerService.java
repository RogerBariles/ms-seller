package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Customer;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.dto.CustomerRequest;
import com.pasteleria.pos.dto.CustomerResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.CustomerRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.security.SecurityUtils;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserService userService;
    private final SaleRepository saleRepository;

    public CustomerService(
            CustomerRepository customerRepository, UserService userService, SaleRepository saleRepository) {
        this.customerRepository = customerRepository;
        this.userService = userService;
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findByDni(String dni) {
        String normalized = normalizeDni(dni);
        if (normalized.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresá al menos 8 dígitos del DNI");
        }
        return customerRepository.findByDni(normalized)
                .map(c -> DtoMapper.toCustomerResponse(c, isEligibleForWelcome(c)));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String dni = normalizeDni(request.dni());
        String whatsapp = blankToNull(request.whatsapp());
        String email = blankToNull(request.email());
        if (whatsapp == null && email == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completá el WhatsApp o el mail (al menos uno)");
        }
        if (customerRepository.existsByDni(dni)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente con ese DNI");
        }
        User user = userService.getUserEntity(SecurityUtils.currentUser().getId());

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFullName(request.fullName().trim());
        customer.setDni(dni);
        customer.setBirthDate(request.birthDate());
        customer.setWhatsapp(whatsapp);
        customer.setEmail(email != null ? email.toLowerCase() : null);
        customer.setCompany(user.getCompany());
        customer.setCreatedBy(user);
        customer.setCreatedAt(OffsetDateTime.now());
        customer.setWelcomePending(true);
        return DtoMapper.toCustomerResponse(customerRepository.save(customer), true);
    }

    /**
     * Bienvenida = cliente dado de alta desde "Asociar cliente" que todavía no compró.
     * Los clientes ya registrados (sin la marca) no reciben el descuento.
     */
    public boolean isFirstPurchase(UUID customerId) {
        return isEligibleForWelcome(getEntity(customerId));
    }

    private boolean isEligibleForWelcome(Customer customer) {
        return customer.isWelcomePending() && !saleRepository.customerHasPurchases(customer.getId());
    }

    public Customer getEntity(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    private static String normalizeDni(String dni) {
        return dni == null ? "" : dni.replaceAll("\\D", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
