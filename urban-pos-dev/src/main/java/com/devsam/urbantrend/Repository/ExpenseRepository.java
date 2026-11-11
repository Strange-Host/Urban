package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    Page<Expense> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Expense> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByRequisition_Id(UUID requisitionId);
    @Query("""
    SELECT COALESCE(SUM(e.amount), 0)
    FROM Expense e
    WHERE e.deletedAt IS NULL
    AND e.createdAt BETWEEN :from AND :to
""")
    BigDecimal sumExpensesBetween(LocalDateTime from, LocalDateTime to);

}

