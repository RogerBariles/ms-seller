package com.pasteleria.pos.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pasteleria.pos.domain.entity.SaleItem;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

    long countByProductId(UUID productId);
}
