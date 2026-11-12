package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.StaffLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface StaffLogRepository extends JpaRepository<StaffLog, java.util.UUID> {

    @Query("""
        SELECT s FROM StaffLog s
        WHERE (:staffId IS NULL OR s.staffId = :staffId)
          AND (:from IS NULL OR s.timestamp >= :from)
          AND (:to IS NULL OR s.timestamp <= :to)
        ORDER BY s.timestamp DESC
    """)
    Page<StaffLog> search(
            @Param("staffId") String staffId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
