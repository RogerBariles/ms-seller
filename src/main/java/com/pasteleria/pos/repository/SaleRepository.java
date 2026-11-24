package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.dto.ReceivableResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Las ventas anuladas no cuentan en totales de ventas, cobros ni acreditaciones.
 * Excepción: las sumas de efectivo por turno/caja incluyen las anuladas, porque ese efectivo
 * entró a la caja; su devolución se registra como un retiro (categoría SALE_CANCELLATION).
 */
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    /** Compras (no anuladas) del cliente: si no tiene ninguna, la próxima es su primera compra. */
    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sale s
            WHERE s.customer.id = :customerId
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    boolean customerHasPurchases(@Param("customerId") UUID customerId);

    @Query("""
            SELECT COALESCE(SUM(s.total), 0) FROM Sale s
            WHERE s.shift.id = :shiftId AND s.paymentMethod = :paymentMethod
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    BigDecimal sumTotalByShiftAndPaymentMethod(
            @Param("shiftId") UUID shiftId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    /** Incluye anuladas: el efectivo entró a la caja (la devolución es un retiro aparte). */
    @Query("SELECT COALESCE(SUM(s.cashAmount), 0) FROM Sale s WHERE s.shift.id = :shiftId")
    BigDecimal sumCashAmountByShift(@Param("shiftId") UUID shiftId);

    /** Incluye anuladas: el efectivo entró a la caja (la devolución es un retiro aparte). */
    @Query("SELECT COALESCE(SUM(s.cashAmount), 0) FROM Sale s WHERE s.shift.cashRegister.id = :cashRegisterId")
    BigDecimal sumCashAmountByCashRegister(@Param("cashRegisterId") UUID cashRegisterId);

    @Query("""
            SELECT COALESCE(SUM(s.total - s.cashAmount), 0) FROM Sale s
            WHERE s.shift.id = :shiftId AND s.paymentMethod = :paymentMethod
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    BigDecimal sumNonCashAmountByShiftAndPaymentMethod(
            @Param("shiftId") UUID shiftId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("""
            SELECT COALESCE(SUM(s.total - s.cashAmount), 0) FROM Sale s
            WHERE s.shift.cashRegister.id = :cashRegisterId AND s.paymentMethod = :paymentMethod
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    BigDecimal sumNonCashAmountByCashRegisterAndPaymentMethod(
            @Param("cashRegisterId") UUID cashRegisterId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("""
            SELECT COUNT(s) FROM Sale s
            WHERE s.shift.id = :shiftId
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    long countByShiftId(@Param("shiftId") UUID shiftId);

    @Query("""
            SELECT COALESCE(SUM(s.total), 0) FROM Sale s
            WHERE s.shift.cashRegister.id = :cashRegisterId AND s.paymentMethod = :paymentMethod
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    BigDecimal sumTotalByCashRegisterAndPaymentMethod(
            @Param("cashRegisterId") UUID cashRegisterId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("""
            SELECT COUNT(s) FROM Sale s
            WHERE s.shift.cashRegister.id = :cashRegisterId
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    long countByCashRegisterId(@Param("cashRegisterId") UUID cashRegisterId);

    /** Trae también las anuladas para mostrarlas en el reporte; los totales las descartan. */
    @Query("""
            SELECT DISTINCT s FROM Sale s
            JOIN FETCH s.seller seller
            LEFT JOIN FETCH seller.company
            LEFT JOIN FETCH s.cancelledBy
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

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED")
    BigDecimal sumAllSales();

    @Query("""
            SELECT COALESCE(SUM(s.total), 0) FROM Sale s
            WHERE s.createdAt >= :from AND s.createdAt <= :to
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    BigDecimal sumSalesBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /** Parte no cobrada en efectivo de las ventas, agrupada por fecha de acreditación. */
    @Query("""
            SELECT new com.pasteleria.pos.dto.ReceivableResponse(
                s.accreditedAt, s.paymentMethod, SUM(s.total - s.cashAmount))
            FROM Sale s
            WHERE s.company.id = :companyId
              AND s.accreditedAt >= :from AND s.accreditedAt <= :to
              AND s.paymentMethod IN :paymentMethods
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            GROUP BY s.accreditedAt, s.paymentMethod
            HAVING SUM(s.total - s.cashAmount) > 0
            ORDER BY s.accreditedAt ASC
            """)
    List<ReceivableResponse> findReceivables(
            @Param("companyId") UUID companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods);

    @Query("""
            SELECT COALESCE(SUM(s.total), 0) FROM Sale s
            WHERE s.createdAt >= :from AND s.createdAt <= :to AND s.company.id = :companyId
              AND s.status <> com.pasteleria.pos.domain.enums.SaleStatus.CANCELLED
            """)
    BigDecimal sumSalesBetweenByCompany(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("companyId") UUID companyId);
}
