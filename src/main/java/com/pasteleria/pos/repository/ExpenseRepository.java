package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.Expense;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal sumAllExpenses();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.createdAt >= :from AND e.createdAt <= :to")
    BigDecimal sumExpensesBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("FROM Expense e WHERE e.createdAt >= :from AND e.createdAt <= :to ORDER BY e.createdAt DESC")
    List<Expense> findBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
