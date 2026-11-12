package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Dtos.TopSellingProductProjection;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Enum.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query(
            value = """
        SELECT o.id FROM Order o
        WHERE (:status IS NULL OR o.orderStatus = :status)
        AND (:staffId IS NULL OR o.createdBy = :staffId)
        AND (:courierId IS NULL OR o.courierId = :courierId)
        AND (:hasQuery = false
            OR LOWER(o.accountNumber) LIKE :pattern
            OR LOWER(o.deliveryCity) LIKE :pattern
            OR LOWER(o.deliveryNotes) LIKE :pattern
            OR o.customerId IN (SELECT c.id FROM Customer c WHERE LOWER(c.name) LIKE :pattern))
    """,
            countQuery = """
        SELECT COUNT(o) FROM Order o
        WHERE (:status IS NULL OR o.orderStatus = :status)
        AND (:staffId IS NULL OR o.createdBy = :staffId)
        AND (:courierId IS NULL OR o.courierId = :courierId)
        AND (:hasQuery = false
            OR LOWER(o.accountNumber) LIKE :pattern
            OR LOWER(o.deliveryCity) LIKE :pattern
            OR LOWER(o.deliveryNotes) LIKE :pattern
            OR o.customerId IN (SELECT c.id FROM Customer c WHERE LOWER(c.name) LIKE :pattern))
    """
    )
    Page<UUID> findAllIds(
            @Param("status") OrderStatus status,
            @Param("staffId") UUID staffId,
            @Param("courierId") UUID courierId,
            @Param("hasQuery") boolean hasQuery,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        WHERE o.id IN :ids
    """)
    List<Order> findAllWithItemsByIds(List<UUID> ids);

    List<Order> findAllByCreatedAtBetweenAndDeletedAtIsNull(LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.createdBy = :staffId
          AND o.orderStatus IN (
              com.devsam.urbantrend.Enum.OrderStatus.DISPATCHED_PAID,
              com.devsam.urbantrend.Enum.OrderStatus.DELIVERED_PAID,
              com.devsam.urbantrend.Enum.OrderStatus.COMPLETED
          )
          AND o.createdAt BETWEEN :from AND :to
    """)
    long countPaidOrders(UUID staffId, LocalDateTime from, LocalDateTime to);

    // Count distinct days where staff made at least 1 PAID order
    @Query("""
        SELECT COUNT(DISTINCT FUNCTION('DATE', o.createdAt))
        FROM Order o
        WHERE o.createdBy = :staffId
          AND o.orderStatus IN (
              com.devsam.urbantrend.Enum.OrderStatus.DISPATCHED_PAID,
              com.devsam.urbantrend.Enum.OrderStatus.DELIVERED_PAID,
              com.devsam.urbantrend.Enum.OrderStatus.COMPLETED
          )
          AND o.createdAt BETWEEN :from AND :to
    """)
    long countPaidDaysWorked(UUID staffId, LocalDateTime from, LocalDateTime to);

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Order> findByAccountNumber(String accountNumber);

    @Modifying
    @Query("""
        UPDATE Order o
        SET o.courierId = NULL, o.courierAssignedAt = NULL
        WHERE o.courierId = :courierId
    """)
    int unassignCourier(@Param("courierId") UUID courierId);


    boolean existsByAccountNumber(String fullAccount);

    @Query(value = """
    SELECT
        p.id AS productId,
        p.name AS productName,
        SUM(oi.quantity) AS totalQuantity
    FROM order_items oi
    JOIN orders o ON oi.order_id = o.id
    JOIN products p ON oi.product_id = p.id
    WHERE o.created_at BETWEEN :from AND :to
      AND o.order_status IN (
          'DISPATCHED_PAID',
          'DISPATCHED_PARTIAL',
          'DISPATCHED_UNPAID',
          'DELIVERED_PAID',
          'DELIVERED_UNPAID',
          'COMPLETED'
      )
      AND o.deleted_at IS NULL
    GROUP BY p.id, p.name
    ORDER BY totalQuantity DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<TopSellingProductProjection> findTopSellingProducts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("limit") int limit
    );

    List<Order> findByCreatedByAndDeletedAtIsNull(UUID staffId);


    @Query("""
    SELECT o FROM Order o
    LEFT JOIN FETCH o.items oi
    LEFT JOIN FETCH oi.product p
    WHERE o.id = :id
""")
    Optional<Order> findByIdWithItemsAndProducts(@Param("id") UUID id);

    // Oldest first, so scanned units go to orders in the order they were placed
    @Query("""
        SELECT DISTINCT o FROM Order o JOIN o.items i
        WHERE o.orderStatus = com.devsam.urbantrend.Enum.OrderStatus.AWAITING_DISPATCH
          AND o.deletedAt IS NULL
          AND i.product.id = :productId
        ORDER BY o.createdAt ASC
    """)
    List<Order> findAwaitingDispatchWithProduct(@Param("productId") UUID productId);
}
