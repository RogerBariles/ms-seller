package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.ProductPriceAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceAuditRepository extends JpaRepository<ProductPriceAudit, UUID> {

    List<ProductPriceAudit> findByProductIdOrderByChangedAtDesc(UUID productId);
}
