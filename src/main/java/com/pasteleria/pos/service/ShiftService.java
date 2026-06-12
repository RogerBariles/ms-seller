package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.dto.CloseReportResponse;
import com.pasteleria.pos.dto.PaymentTotalsResponse;
import com.pasteleria.pos.dto.ShiftResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ShiftRepository shiftRepository;
    private final CashRegisterService cashRegisterService;
    private final SaleRepository saleRepository;
    private final UserService userService;
    private final CloseReportBuilder closeReportBuilder;

    public ShiftService(
            ShiftRepository shiftRepository,
            CashRegisterService cashRegisterService,
            SaleRepository saleRepository,
            UserService userService,
            CloseReportBuilder closeReportBuilder) {
        this.shiftRepository = shiftRepository;
        this.cashRegisterService = cashRegisterService;
        this.saleRepository = saleRepository;
        this.userService = userService;
        this.closeReportBuilder = closeReportBuilder;
    }

    @Transactional(readOnly = true)
    public Optional<ShiftResponse> getActiveShift() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return shiftRepository.findBySellerIdAndStatus(principal.getId(), ShiftStatus.OPEN)
                .map(DtoMapper::toShiftResponse);
    }

    @Transactional
    public ShiftResponse startShift() {
        UserPrincipal principal = SecurityUtils.currentUser();
        shiftRepository.findBySellerIdAndStatus(principal.getId(), ShiftStatus.OPEN)
                .ifPresent(shift -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Ya tiene un turno abierto");
                });

        CashRegister cashRegister = cashRegisterService.getOpenCashRegisterForToday();
        User seller = userService.getUserEntity(principal.getId());

        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCashRegister(cashRegister);
        shift.setSeller(seller);
        shift.setInitialCash(cashRegister.getInitialCash());
        shift.setStatus(ShiftStatus.OPEN);
        shift.setStartedAt(OffsetDateTime.now(ZONE));
        return DtoMapper.toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public CloseReportResponse closeShift(UUID id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        Shift shift = shiftRepository.findByIdForClose(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        if (!shift.getSeller().getId().equals(principal.getId())
                && principal.getRole() != com.pasteleria.pos.domain.enums.UserRole.SUPER_ADMIN
                && principal.getRole() != com.pasteleria.pos.domain.enums.UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puede cerrar este turno");
        }
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "El turno ya está cerrado");
        }

        User closedBy = userService.getUserEntity(principal.getId());
        OffsetDateTime closedAt = OffsetDateTime.now(ZONE);
        CashRegister cashRegister = shift.getCashRegister();
        PaymentTotalsResponse paymentTotals = closeReportBuilder.paymentTotalsByShift(shift.getId());
        long salesCount = saleRepository.countByShiftId(shift.getId());
        BigDecimal totalSales = CloseReportBuilder.totalAmount(paymentTotals);
        BigDecimal cashSales = paymentTotals.cash();
        BigDecimal finalCash = shift.getInitialCash().add(cashSales);

        shift.setCashSalesTotal(cashSales);
        shift.setSalesCount((int) salesCount);
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setEndedAt(closedAt);
        shiftRepository.save(shift);

        return new CloseReportResponse(
                "SHIFT",
                cashRegister.getOpenedAt(),
                cashRegister.getOpenedBy().getName(),
                cashRegister.getInitialCash(),
                null,
                null,
                shift.getStartedAt(),
                shift.getSeller().getName(),
                shift.getInitialCash(),
                closedAt,
                closedBy.getName(),
                shift.getInitialCash(),
                finalCash,
                salesCount,
                totalSales,
                paymentTotals);
    }

    public Shift getRequiredActiveShiftForSeller(UUID sellerId) {
        return shiftRepository.findBySellerIdAndStatus(sellerId, ShiftStatus.OPEN)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Debe tener caja y turno abiertos para vender"));
    }
}
