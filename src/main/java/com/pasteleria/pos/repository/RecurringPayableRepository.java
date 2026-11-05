package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.RecurringPayable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringPayableRepository extends JpaRepository<RecurringPayable, UUID> {

    Optional<RecurringPayable> findByIdAndCompanyId(UUID id, UUID companyId);

    List<RecurringPayable> findByCompanyIdOrderByDayOfMonthAsc(UUID companyId);

    List<RecurringPayable> findByCompanyIdAndActiveTrue(UUID companyId);
}
