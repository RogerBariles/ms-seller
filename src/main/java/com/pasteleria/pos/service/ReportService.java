package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.dto.SalesReportResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
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
            LocalTime fromTime,
            LocalTime toTime,
            PaymentMethod paymentMethod,
            UUID sellerId,
            UUID companyId) {
        if (fromDate == null || toDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar rango de fechas");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }

        LocalTime startTime = fromTime != null ? fromTime : LocalTime.MIN;
        LocalTime endTime = toTime != null ? toTime : LocalTime.MAX;
        OffsetDateTime from = OffsetDateTime.of(fromDate, startTime, ZONE.getRules().getOffset(fromDate.atStartOfDay()));
        OffsetDateTime to = OffsetDateTime.of(toDate, endTime, ZONE.getRules().getOffset(toDate.atStartOfDay()));

        List<Sale> sales = saleRepository.findForReport(from, to, paymentMethod, sellerId, companyId);
        List<SaleResponse> saleResponses = sales.stream().map(DtoMapper::toSaleResponse).toList();

        Map<PaymentMethod, BigDecimal> amountByPaymentMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            amountByPaymentMethod.put(method, BigDecimal.ZERO.setScale(2));
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Sale sale : sales) {
            totalAmount = totalAmount.add(sale.getTotal());
            amountByPaymentMethod.merge(sale.getPaymentMethod(), sale.getTotal(), BigDecimal::add);
        }

        return new SalesReportResponse(sales.size(), totalAmount.setScale(2), amountByPaymentMethod, saleResponses);
    }
}
