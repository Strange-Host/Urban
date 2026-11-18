package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.SalaryAdvance;
import com.devsam.urbantrend.Enum.RequisitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, UUID> {
    boolean existsByRequisition_Id(UUID requisitionId);

    // An advance row is only created when its requisition is marked PAID, so createdAt is the
    // day the money was handed over. The status check keeps it correct if that ever changes.
    @Query("""
        SELECT COALESCE(SUM(a.amount), 0)
        FROM SalaryAdvance a
        WHERE a.user.id = :userId
          AND a.deletedAt IS NULL
          AND a.requisition.status = :status
          AND a.createdAt BETWEEN :from AND :to
    """)
    BigDecimal sumAdvancesForUser(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") RequisitionStatus status
    );

    @Query("""
        SELECT a.user.id, SUM(a.amount)
        FROM SalaryAdvance a
        WHERE a.deletedAt IS NULL
          AND a.requisition.status = :status
          AND a.createdAt BETWEEN :from AND :to
        GROUP BY a.user.id
    """)
    List<Object[]> sumAdvancesByUser(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") RequisitionStatus status
    );
}
