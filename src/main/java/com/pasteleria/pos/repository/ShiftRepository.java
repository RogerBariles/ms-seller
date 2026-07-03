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
              AND s.company.id = :companyId
            """)
    Optional<Shift> findBySellerIdAndStatus(
            @Param("sellerId") UUID sellerId,
            @Param("status") ShiftStatus status,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.seller
            JOIN FETCH s.cashRegister
            WHERE s.seller.id = :sellerId AND s.status = :status
              AND s.company.id = :companyId
            """)
    List<Shift> findBySellerIdAndStatusList(
            @Param("sellerId") UUID sellerId,
            @Param("status") ShiftStatus status,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.cashRegister cr
            WHERE cr.id = :cashRegisterId
              AND s.company.id = :companyId
            ORDER BY s.startedAt ASC
            """)
    List<Shift> findByCashRegisterIdOrderByStartedAtAsc(
            @Param("cashRegisterId") UUID cashRegisterId,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Shift s
            WHERE s.cashRegister.id = :cashRegisterId
              AND s.company.id = :companyId
            """)
    boolean existsByCashRegisterId(
            @Param("cashRegisterId") UUID cashRegisterId,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Shift s
            WHERE s.cashRegister.id = :cashRegisterId AND s.status = :status
              AND s.company.id = :companyId
            """)
    boolean existsByCashRegisterIdAndStatus(
            @Param("cashRegisterId") UUID cashRegisterId,
            @Param("status") ShiftStatus status,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.seller
            JOIN FETCH s.cashRegister
            WHERE s.status = :status
              AND s.company.id = :companyId
            ORDER BY s.startedAt ASC
            """)
    Optional<Shift> findFirstByStatusOrderByStartedAtAsc(
            @Param("status") ShiftStatus status,
            @Param("companyId") UUID companyId);

    // Un-scoped version for background tasks (BirthdaySaleScheduler) with no security context
    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.seller
            JOIN FETCH s.cashRegister
            WHERE s.status = :status
            ORDER BY s.startedAt ASC
            """)
    Optional<Shift> findFirstByStatusOrderByStartedAtAscUnscoped(@Param("status") ShiftStatus status);

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
              AND s.company.id = :companyId
            ORDER BY s.startedAt DESC
            """)
    List<Shift> findByCashRegisterBusinessDateDesc(
            @Param("businessDate") LocalDate businessDate,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Shift s
            WHERE s.id = :id AND s.company.id = :companyId
            """)
    boolean existsByIdAndCompanyId(@Param("id") UUID id, @Param("companyId") UUID companyId);
}
