package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByDni(String dni);

    boolean existsByDni(String dni);

    @Query("""
            SELECT c FROM Customer c
            LEFT JOIN FETCH c.company
            LEFT JOIN FETCH c.createdBy
            ORDER BY lower(c.fullName)
            """)
    List<Customer> findAllWithCompanyAndCreator();
}
