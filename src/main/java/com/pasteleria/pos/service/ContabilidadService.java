package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Expense;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.dto.ContabilidadResponse;
import com.pasteleria.pos.dto.CreateExpenseRequest;
import com.pasteleria.pos.dto.ExpenseResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.ExpenseRepository;
import com.pasteleria.pos.repository.SaleRepository;
import com.pasteleria.pos.repository.UserRepository;
import com.pasteleria.pos.security.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContabilidadService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ContabilidadService(SaleRepository saleRepository, ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ContabilidadResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime from = OffsetDateTime.of(fromDate, LocalTime.MIN, ZONE.getRules().getOffset(fromDate.atStartOfDay()));
        OffsetDateTime to = OffsetDateTime.of(toDate, LocalTime.MAX, ZONE.getRules().getOffset(toDate.atStartOfDay()));

        BigDecimal totalSales = saleRepository.sumSalesBetween(from, to);
        BigDecimal totalExpenses = expenseRepository.sumExpensesBetween(from, to);
        BigDecimal netAmount = totalSales.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);

        List<Expense> expenses = expenseRepository.findBetween(from, to);
        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(e -> new ExpenseResponse(
                        e.getId(),
                        e.getDetail(),
                        e.getAmount(),
                        e.getCreatedBy().getName(),
                        e.getCreatedAt()))
                .toList();

        return new ContabilidadResponse(
            totalSales.setScale(2, RoundingMode.HALF_UP),
            totalExpenses.setScale(2, RoundingMode.HALF_UP),
            netAmount,
            expenseResponses);
    }

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {
        if (request.detail() == null || request.detail().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El detalle es obligatorio");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        }

        UUID userId = SecurityUtils.currentUser().getId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Expense expense = new Expense(UUID.randomUUID(), request.detail().trim(), request.amount(), user);
        expense = expenseRepository.save(expense);

        return new ExpenseResponse(
            expense.getId(),
            expense.getDetail(),
            expense.getAmount(),
            expense.getCreatedBy().getName(),
            expense.getCreatedAt());
    }
}
