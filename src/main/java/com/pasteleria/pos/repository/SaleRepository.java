package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @Query("""
            SELECT COALESCE(SUM(s.total), 0) FROM Sale s
            WHERE s.shift.id = :shiftId AND s.paymentMethod = :paymentMethod
            """)
    BigDecimal sumTotalByShiftAndPaymentMethod(
            @Param("shiftId") UUID shiftId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.shift.id = :shiftId")
    long countByShiftId(@Param("shiftId") UUID shiftId);

    @Query("""
            SELECT COALESCE(SUM(s.total), 0) FROM Sale s
            WHERE s.shift.cashRegister.id = :cashRegisterId AND s.paymentMethod = :paymentMethod
            """)
    BigDecimal sumTotalByCashRegisterAndPaymentMethod(
            @Param("cashRegisterId") UUID cashRegisterId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.shift.cashRegister.id = :cashRegisterId")
    long countByCashRegisterId(@Param("cashRegisterId") UUID cashRegisterId);

    @Query("""
            SELECT DISTINCT s FROM Sale s
            JOIN FETCH s.seller seller
            LEFT JOIN FETCH seller.company
            JOIN FETCH s.items item
            JOIN FETCH item.product
            WHERE s.createdAt >= :from AND s.createdAt <= :to
              AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod)
              AND (:sellerId IS NULL OR s.seller.id = :sellerId)
              AND (:companyId IS NULL OR seller.company.id = :companyId)
            ORDER BY s.createdAt DESC
            """)
    List<Sale> findForReport(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("sellerId") UUID sellerId,
            @Param("companyId") UUID companyId);

    boolean existsBySellerIdAndBirthdayTrueAndCreatedAtBetween(
            UUID sellerId, OffsetDateTime from, OffsetDateTime to);
}
