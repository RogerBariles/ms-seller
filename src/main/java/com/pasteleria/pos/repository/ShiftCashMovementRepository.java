package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.ShiftCashMovement;
import com.pasteleria.pos.domain.enums.CashMovementType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftCashMovementRepository extends JpaRepository<ShiftCashMovement, UUID> {

    @Query("""
            SELECT m FROM ShiftCashMovement m
            JOIN FETCH m.createdBy
            WHERE m.shift.id = :shiftId
            ORDER BY m.createdAt ASC
            """)
    List<ShiftCashMovement> findByShiftIdOrderByCreatedAtAsc(@Param("shiftId") UUID shiftId);

    @Query("""
            SELECT COALESCE(SUM(m.amount), 0) FROM ShiftCashMovement m
            WHERE m.shift.id = :shiftId AND m.movementType = :type
            """)
    BigDecimal sumByShiftIdAndType(
            @Param("shiftId") UUID shiftId,
            @Param("type") CashMovementType type);
}
