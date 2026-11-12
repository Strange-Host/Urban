package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    Page<Lead> findAllByStatus(String status, Pageable pageable);

    boolean existsByWpEntryId(Long wpEntryId);

    Page<Lead> findByStatus(String status, Pageable pageable);

    Page<Lead> findByAssignedTo(User assignedTo, Pageable pageable);

    Page<Lead> findByAssignedToAndStatus(User assignedTo, String status, Pageable pageable);

    List<Lead> findByAssignedToIsNullAndStatus(String status);

    @Query("""
        SELECT l FROM Lead l
        WHERE (l.assignedTo = :user OR LOWER(l.formName) IN :productNames)
    """)
    Page<Lead> findByAssignedToOrFormNameIn(
            @org.springframework.data.repository.query.Param("user") User user,
            @org.springframework.data.repository.query.Param("productNames") List<String> productNames,
            Pageable pageable
    );

    @Query("""
        SELECT l FROM Lead l
        WHERE (l.assignedTo = :user OR LOWER(l.formName) IN :productNames)
          AND l.status = :status
    """)
    Page<Lead> findByAssignedToOrFormNameInAndStatus(
            @org.springframework.data.repository.query.Param("user") User user,
            @org.springframework.data.repository.query.Param("productNames") List<String> productNames,
            @org.springframework.data.repository.query.Param("status") String status,
            Pageable pageable
    );

    List<Lead> findByFollowUpAtBeforeAndNotifiedFalseAndStatusNot(LocalDateTime now, String converted);

    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.assignedTo.id = :staffId
          AND l.created BETWEEN :from AND :to
    """)
    long countAssignedLeads(
            @org.springframework.data.repository.query.Param("staffId") java.util.UUID staffId,
            @org.springframework.data.repository.query.Param("from") LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") LocalDateTime to
    );

    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.assignedTo.id = :staffId
          AND l.created BETWEEN :from AND :to
          AND l.status = 'CONVERTED'
          AND l.convertedOrderId IN (
              SELECT o.id FROM com.devsam.urbantrend.Entity.Order o
              WHERE o.orderStatus = com.devsam.urbantrend.Enum.OrderStatus.DELIVERED_PAID
          )
    """)
    long countConvertedAndPaidLeads(
            @org.springframework.data.repository.query.Param("staffId") java.util.UUID staffId,
            @org.springframework.data.repository.query.Param("from") LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") LocalDateTime to
    );
}