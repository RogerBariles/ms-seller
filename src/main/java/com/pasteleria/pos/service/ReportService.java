package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.SaleItem;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.dto.SalesReportResponse;
import com.pasteleria.pos.dto.TopDayResponse;
import com.pasteleria.pos.dto.TopProductResponse;
import com.pasteleria.pos.dto.TopSellerResponse;
import com.pasteleria.pos.dto.TopStatsResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.security.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final SaleRepository saleRepository;

    public ReportService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(
            LocalDate fromDate,
            LocalDate toDate,
            PaymentMethod paymentMethod,
            UUID sellerId,
            UUID companyId,
            ProductCategory category) {
        if (fromDate == null || toDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar rango de fechas");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }

        OffsetDateTime from = OffsetDateTime.of(fromDate, LocalTime.MIN, ZONE.getRules().getOffset(fromDate.atStartOfDay()));
        OffsetDateTime to = OffsetDateTime.of(toDate, LocalTime.MAX, ZONE.getRules().getOffset(toDate.atStartOfDay()));

        UUID resolvedCompanyId = resolveCompanyId(companyId);
        UUID resolvedSellerId = resolveSellerId(sellerId);
        List<Sale> sales = saleRepository.findForReport(from, to, paymentMethod, resolvedSellerId, resolvedCompanyId, category);
        List<SaleResponse> saleResponses = sales.stream().map(DtoMapper::toSaleResponse).toList();

        Map<PaymentMethod, BigDecimal> amountByPaymentMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            amountByPaymentMethod.put(method, BigDecimal.ZERO.setScale(2));
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Sale sale : sales) {
            totalAmount = totalAmount.add(sale.getTotal());
            BigDecimal cashPortion = sale.getCashAmount();
            BigDecimal nonCashPortion = sale.getTotal().subtract(cashPortion);
            amountByPaymentMethod.merge(PaymentMethod.EFECTIVO, cashPortion, BigDecimal::add);
            amountByPaymentMethod.merge(sale.getPaymentMethod(), nonCashPortion, BigDecimal::add);
            for (var item : sale.getItems()) {
                totalCost = totalCost.add(
                        item.getUnitPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalProfit = totalAmount.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);

        return new SalesReportResponse(
                sales.size(),
                totalAmount.setScale(2, RoundingMode.HALF_UP),
                totalCost,
                totalProfit,
                amountByPaymentMethod,
                saleResponses);
    }

    @Transactional(readOnly = true)
    public TopStatsResponse getTopStats(
            LocalDate fromDate,
            LocalDate toDate,
            PaymentMethod paymentMethod,
            UUID sellerId,
            UUID companyId,
            ProductCategory category) {
        if (fromDate == null || toDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar rango de fechas");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }

        OffsetDateTime from = OffsetDateTime.of(fromDate, LocalTime.MIN, ZONE.getRules().getOffset(fromDate.atStartOfDay()));
        OffsetDateTime to = OffsetDateTime.of(toDate, LocalTime.MAX, ZONE.getRules().getOffset(toDate.atStartOfDay()));

        UUID resolvedCompanyId = resolveCompanyId(companyId);
        UUID resolvedSellerId = resolveSellerId(sellerId);
        List<Sale> sales = saleRepository.findForReport(from, to, paymentMethod, resolvedSellerId, resolvedCompanyId, category);

        // Top 10 best-selling products
        Map<String, Long> productQuantityMap = new HashMap<>();
        for (Sale sale : sales) {
            for (SaleItem item : sale.getItems()) {
                productQuantityMap.merge(item.getProductName(), item.getQuantity().longValue(), Long::sum);
            }
        }
        List<TopProductResponse> topProducts = productQuantityMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new TopProductResponse(e.getKey(), e.getValue()))
                .toList();

        // Top 10 days by total item quantity sold
        Map<LocalDate, Long> dayQuantityMap = new HashMap<>();
        for (Sale sale : sales) {
            LocalDate date = sale.getCreatedAt().atZoneSameInstant(ZONE).toLocalDate();
            long dayQty = sale.getItems().stream().mapToLong(SaleItem::getQuantity).sum();
            dayQuantityMap.merge(date, dayQty, Long::sum);
        }
        List<TopDayResponse> topDays = dayQuantityMap.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new TopDayResponse(e.getKey(), e.getValue()))
                .toList();

        // Top 10 sellers by total sales amount
        Map<String, BigDecimal> sellerAmountMap = new HashMap<>();
        Map<String, Long> sellerCountMap = new HashMap<>();
        for (Sale sale : sales) {
            String name = sale.getSeller().getName();
            sellerAmountMap.merge(name, sale.getTotal(), BigDecimal::add);
            sellerCountMap.merge(name, 1L, Long::sum);
        }
        List<TopSellerResponse> topSellers = sellerAmountMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(e -> new TopSellerResponse(e.getKey(), sellerCountMap.get(e.getKey()), e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        return new TopStatsResponse(topProducts, topDays, topSellers);
    }

    private static UUID resolveCompanyId(UUID companyId) {
        if (companyId != null) {
            return companyId;
        }
        return SecurityUtils.currentUser().getCompanyId();
    }

    private static UUID resolveSellerId(UUID sellerId) {
        var principal = SecurityUtils.currentUser();
        if (principal.getRole() == UserRole.SELLER) {
            return principal.getId();
        }
        return sellerId;
    }
}
