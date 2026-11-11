package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    Order create(OrderRequest request, User user);

    Order getById(UUID id);

    Page<OrderResponse> getAll(OrderStatus status, UUID staffId, UUID courierId, String query, Pageable pageable);

    Order updateStatus(UUID id, OrderStatus status, User user);

    BigDecimal getOrderRevenue(UUID orderId);

    BigDecimal getOrderCogs(UUID orderId);

    BigDecimal getOrderGrossProfit(UUID orderId);

    List<TopSellingProductResponse> getTopSellingProducts(
            LocalDate from,
            LocalDate to,
            int limit
    );

    StaffOrderSummary getStaffSummary(UUID staffId);

    Order updateNotes(UUID id, UpdateOrderNotesRequest request, User user);

    Order assignCourier(UUID id, AssignCourierRequest request, User user);

}


