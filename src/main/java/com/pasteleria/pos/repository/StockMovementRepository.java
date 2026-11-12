package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.StockMovement;
import com.pasteleria.pos.domain.enums.StockReferenceType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<StockMovement> findByReferenceTypeAndReferenceId(StockReferenceType referenceType, UUID referenceId);
}
