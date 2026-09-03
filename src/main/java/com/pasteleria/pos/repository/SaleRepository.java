package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ProductCategory;
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

    @Query("SELECT COALESCE(SUM(s.cashAmount), 0) FROM Sale s WHERE s.shift.id = :shiftId")
    BigDecimal sumCashAmountByShift(@Param("shiftId") UUID shiftId);

    @Query("SELECT COALESCE(SUM(s.cashAmount), 0) FROM Sale s WHERE s.shift.cashRegister.id = :cashRegisterId")
    BigDecimal sumCashAmountByCashRegister(@Param("cashRegisterId") UUID cashRegisterId);

    @Query("""
            SELECT COALESCE(SUM(s.total - s.cashAmount), 0) FROM Sale s
            WHERE s.shift.id = :shiftId AND s.paymentMethod = :paymentMethod
            """)
    BigDecimal sumNonCashAmountByShiftAndPaymentMethod(
            @Param("shiftId") UUID shiftId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("""
            SELECT COALESCE(SUM(s.total - s.cashAmount), 0) FROM Sale s
            WHERE s.shift.cashRegister.id = :cashRegisterId AND s.paymentMethod = :paymentMethod
            """)
    BigDecimal sumNonCashAmountByCashRegisterAndPaymentMethod(
            @Param("cashRegisterId") UUID cashRegisterId,
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
              AND s.paymentMethod IN :paymentMethods
              AND (:sellerId IS NULL OR s.seller.id = :sellerId)
              AND (:companyId IS NULL OR seller.company.id = :companyId)
              AND item.product.category IN :categories
            ORDER BY s.createdAt DESC
            """)
    List<Sale> findForReport(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods,
            @Param("sellerId") UUID sellerId,
            @Param("companyId") UUID companyId,
            @Param("categories") List<ProductCategory> categories);

    boolean existsBySellerIdAndBirthdayTrueAndCreatedAtBetween(
            UUID sellerId, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s")
    BigDecimal sumAllSales();

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.createdAt >= :from AND s.createdAt <= :to")
    BigDecimal sumSalesBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.createdAt >= :from AND s.createdAt <= :to AND s.company.id = :companyId")
    BigDecimal sumSalesBetweenByCompany(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("companyId") UUID companyId);
}
