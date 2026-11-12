package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.OrderItem;
import com.devsam.urbantrend.Enum.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrder_Id(UUID orderId);

    @Query("""
        SELECT COALESCE(SUM(oi.lineTotal), 0),
               COALESCE(SUM(oi.costPrice * oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.order.deletedAt IS NULL
          AND oi.order.createdAt BETWEEN :from AND :to
          AND oi.order.orderStatus IN :statuses
    """)
    Object[] sumRevenueAndCogs(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("statuses") Collection<OrderStatus> statuses
    );
}
