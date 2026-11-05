package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Payable;
import com.pasteleria.pos.domain.enums.PayableStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableRepository extends JpaRepository<Payable, UUID> {

    Optional<Payable> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Payable> findByCompanyIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(
            UUID companyId, PayableStatus status, LocalDate dueDate);

    boolean existsByRecurringIdAndPeriod(UUID recurringId, LocalDate period);
}
