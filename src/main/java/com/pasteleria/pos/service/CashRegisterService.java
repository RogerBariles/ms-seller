package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.dto.CashRegisterResponse;
import com.pasteleria.pos.dto.CloseReportResponse;
import com.pasteleria.pos.dto.OpenCashRegisterRequest;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.CashRegisterRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashRegisterService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final CashRegisterRepository cashRegisterRepository;
    private final ShiftRepository shiftRepository;
    private final SaleRepository saleRepository;
    private final UserService userService;

    public CashRegisterService(
            CashRegisterRepository cashRegisterRepository,
            ShiftRepository shiftRepository,
            SaleRepository saleRepository,
            UserService userService) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.shiftRepository = shiftRepository;
        this.saleRepository = saleRepository;
        this.userService = userService;
    }

    /** Caja abierta actual del día (si hay varias cerradas, devuelve la que está OPEN). */
    @Transactional(readOnly = true)
    public Optional<CashRegisterResponse> getTodayCashRegister() {
        return findOpenCashRegisterForToday()
                .map(DtoMapper::toCashRegisterResponse);
    }

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> getTodayCashRegisterHistory() {
        return cashRegisterRepository.findAllByBusinessDateOrderByOpenedAtDesc(today()).stream()
                .map(DtoMapper::toCashRegisterResponse)
                .toList();
    }

    @Transactional
    public CashRegisterResponse openCashRegister(OpenCashRegisterRequest request) {
        LocalDate today = today();
        if (cashRegisterRepository.existsByBusinessDateAndStatus(today, CashRegisterStatus.OPEN)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ya hay una caja abierta. Cierre la caja actual antes de abrir otra.");
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());

        CashRegister cashRegister = new CashRegister();
        cashRegister.setId(UUID.randomUUID());
        cashRegister.setBusinessDate(today);
        cashRegister.setInitialCash(request.initialCash());
        cashRegister.setStatus(CashRegisterStatus.OPEN);
        cashRegister.setOpenedBy(user);
        cashRegister.setOpenedAt(OffsetDateTime.now(ZONE));
        return DtoMapper.toCashRegisterResponse(cashRegisterRepository.save(cashRegister));
    }

    @Transactional
    public CloseReportResponse closeCashRegister(UUID id) {
        CashRegister cashRegister = cashRegisterRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caja no encontrada"));
        if (cashRegister.getStatus() == CashRegisterStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "La caja ya está cerrada");
        }
        if (shiftRepository.existsByCashRegisterIdAndStatus(cashRegister.getId(), ShiftStatus.OPEN)) {
            throw new ApiException(HttpStatus.CONFLICT, "Hay turnos abiertos. Cierre todos los turnos primero");
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        cashRegister.setStatus(CashRegisterStatus.CLOSED);
        cashRegister.setClosedBy(user);
        cashRegister.setClosedAt(OffsetDateTime.now(ZONE));
        cashRegisterRepository.save(cashRegister);

        BigDecimal cashSales = saleRepository.sumTotalByCashRegisterAndPaymentMethod(
                cashRegister.getId(), PaymentMethod.EFECTIVO);
        long salesCount = saleRepository.countByCashRegisterId(cashRegister.getId());
        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        BigDecimal totalSales = saleRepository.findForReport(
                        cashRegister.getOpenedAt(),
                        OffsetDateTime.now(ZONE),
                        null,
                        null,
                        companyId)
                .stream()
                .map(sale -> sale.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CloseReportResponse(
                cashRegister.getInitialCash(),
                cashSales,
                salesCount,
                totalSales);
    }

    public CashRegister getOpenCashRegisterForToday() {
        return findOpenCashRegisterForToday()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No hay caja abierta para hoy"));
    }

    private Optional<CashRegister> findOpenCashRegisterForToday() {
        return cashRegisterRepository.findFirstByBusinessDateAndStatusOrderByOpenedAtDesc(
                today(), CashRegisterStatus.OPEN);
    }

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
