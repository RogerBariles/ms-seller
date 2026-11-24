package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByDni(String dni);

    boolean existsByDni(String dni);
}
