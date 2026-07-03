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
              AND cr.company.id = :companyId
            ORDER BY cr.openedAt DESC
            """)
    Optional<CashRegister> findFirstByBusinessDateAndStatusOrderByOpenedAtDesc(
            @Param("businessDate") LocalDate businessDate,
            @Param("status") CashRegisterStatus status,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT cr FROM CashRegister cr
            JOIN FETCH cr.openedBy
            LEFT JOIN FETCH cr.closedBy
            WHERE cr.businessDate = :businessDate
              AND cr.company.id = :companyId
            ORDER BY cr.openedAt DESC
            """)
    List<CashRegister> findAllByBusinessDateOrderByOpenedAtDesc(
            @Param("businessDate") LocalDate businessDate,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END
            FROM CashRegister cr
            WHERE cr.businessDate = :businessDate AND cr.status = :status
              AND cr.company.id = :companyId
            """)
    boolean existsByBusinessDateAndStatus(
            @Param("businessDate") LocalDate businessDate,
            @Param("status") CashRegisterStatus status,
            @Param("companyId") UUID companyId);

    @Query("""
            SELECT cr FROM CashRegister cr
            JOIN FETCH cr.openedBy
            LEFT JOIN FETCH cr.closedBy
            WHERE cr.id = :id
            """)
    Optional<CashRegister> findByIdWithUsers(@Param("id") UUID id);

    @Query("""
            SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END
            FROM CashRegister cr
            WHERE cr.id = :id AND cr.company.id = :companyId
            """)
    boolean existsByIdAndCompanyId(@Param("id") UUID id, @Param("companyId") UUID companyId);
}
