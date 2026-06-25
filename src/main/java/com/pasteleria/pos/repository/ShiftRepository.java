package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.seller
            JOIN FETCH s.cashRegister
            WHERE s.seller.id = :sellerId AND s.status = :status
            """)
    Optional<Shift> findBySellerIdAndStatus(
            @Param("sellerId") UUID sellerId,
            @Param("status") ShiftStatus status);

    List<Shift> findByCashRegisterIdOrderByStartedAtAsc(UUID cashRegisterId);

    boolean existsByCashRegisterId(UUID cashRegisterId);

    boolean existsByCashRegisterIdAndStatus(UUID cashRegisterId, ShiftStatus status);

    Optional<Shift> findFirstByStatusOrderByStartedAtAsc(ShiftStatus status);

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.seller seller
            LEFT JOIN FETCH seller.company
            WHERE s.id = :id
            """)
    Optional<Shift> findByIdWithSellerAndCompany(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.seller seller
            LEFT JOIN FETCH seller.company
            JOIN FETCH s.cashRegister cr
            JOIN FETCH cr.openedBy
            WHERE s.id = :id
            """)
    Optional<Shift> findByIdForClose(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.seller
            JOIN FETCH s.cashRegister cr
            WHERE cr.businessDate = :businessDate
            ORDER BY s.startedAt DESC
            """)
    List<Shift> findByCashRegisterBusinessDateDesc(@Param("businessDate") LocalDate businessDate);
}
