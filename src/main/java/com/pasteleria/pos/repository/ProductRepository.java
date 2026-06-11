package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.enums.ProductCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findAllByOrderByNameAsc();

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:pattern IS NULL OR LOWER(p.name) LIKE :pattern)
              AND (:category IS NULL OR p.category = :category)
            ORDER BY p.name ASC
            """)
    List<Product> search(
            @Param("pattern") String pattern,
            @Param("category") ProductCategory category);

    List<Product> findByActiveTrue();
}
