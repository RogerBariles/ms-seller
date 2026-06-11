package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashRegisterRepository extends JpaRepository<CashRegister, UUID> {

    @Query("""
            SELECT cr FROM CashRegister cr
            JOIN FETCH cr.openedBy
            WHERE cr.businessDate = :businessDate AND cr.status = :status
            ORDER BY cr.openedAt DESC
            """)
    Optional<CashRegister> findFirstByBusinessDateAndStatusOrderByOpenedAtDesc(
            @Param("businessDate") LocalDate businessDate,
            @Param("status") CashRegisterStatus status);

    @Query("""
            SELECT cr FROM CashRegister cr
            JOIN FETCH cr.openedBy
            WHERE cr.businessDate = :businessDate
            ORDER BY cr.openedAt DESC
            """)
    List<CashRegister> findAllByBusinessDateOrderByOpenedAtDesc(@Param("businessDate") LocalDate businessDate);

    boolean existsByBusinessDateAndStatus(LocalDate businessDate, CashRegisterStatus status);
}
