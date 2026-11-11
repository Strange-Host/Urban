package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrder_Id(UUID orderId);
}
