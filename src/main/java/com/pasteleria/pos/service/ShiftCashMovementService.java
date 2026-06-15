package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.ShiftCashMovement;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashMovementType;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.dto.ShiftCashMovementRequest;
import com.pasteleria.pos.dto.ShiftCashMovementResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.ShiftCashMovementRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftCashMovementService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ShiftCashMovementRepository movementRepository;
    private final ShiftRepository shiftRepository;
    private final SaleRepository saleRepository;
    private final UserService userService;

    public ShiftCashMovementService(
            ShiftCashMovementRepository movementRepository,
            ShiftRepository shiftRepository,
            SaleRepository saleRepository,
            UserService userService) {
        this.movementRepository = movementRepository;
        this.shiftRepository = shiftRepository;
        this.saleRepository = saleRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ShiftCashMovementResponse> listMovements(UUID shiftId) {
        return movementRepository.findByShiftIdOrderByCreatedAtAsc(shiftId).stream()
                .map(DtoMapper::toShiftCashMovementResponse)
                .toList();
    }

    @Transactional
    public ShiftCashMovementResponse addMovement(UUID shiftId, ShiftCashMovementRequest request) {
        Shift shift = getOpenShiftForCurrentUser(shiftId);
        if (request.type() == CashMovementType.WITHDRAWAL) {
            BigDecimal available = availableCash(shift.getId(), shift.getInitialCash());
            if (available.compareTo(request.amount()) < 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "No hay efectivo suficiente para el retiro. Disponible: " + available);
            }
        }

        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());

        ShiftCashMovement movement = new ShiftCashMovement();
        movement.setId(UUID.randomUUID());
        movement.setShift(shift);
        movement.setMovementType(request.type());
        movement.setAmount(request.amount());
        movement.setDetail(request.detail().trim());
        movement.setCreatedBy(user);
        movement.setCreatedAt(OffsetDateTime.now(ZONE));
        return DtoMapper.toShiftCashMovementResponse(movementRepository.save(movement));
    }

    public BigDecimal sumIncome(UUID shiftId) {
        return movementRepository.sumByShiftIdAndType(shiftId, CashMovementType.INCOME);
    }

    public BigDecimal sumWithdrawal(UUID shiftId) {
        return movementRepository.sumByShiftIdAndType(shiftId, CashMovementType.WITHDRAWAL);
    }

    public BigDecimal expectedFinalCash(UUID shiftId, BigDecimal initialCash, BigDecimal cashSales) {
        return initialCash
                .add(cashSales)
                .add(sumIncome(shiftId))
                .subtract(sumWithdrawal(shiftId));
    }

    public BigDecimal availableCash(UUID shiftId, BigDecimal initialCash) {
        BigDecimal cashSales = saleRepository.sumTotalByShiftAndPaymentMethod(
                shiftId, com.pasteleria.pos.domain.enums.PaymentMethod.EFECTIVO);
        return expectedFinalCash(shiftId, initialCash, cashSales);
    }

    private Shift getOpenShiftForCurrentUser(UUID shiftId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "El turno no está abierto");
        }
        if (!shift.getSeller().getId().equals(principal.getId())
                && principal.getRole() != com.pasteleria.pos.domain.enums.UserRole.SUPER_ADMIN
                && principal.getRole() != com.pasteleria.pos.domain.enums.UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puede operar sobre este turno");
        }
        return shift;
    }
}
