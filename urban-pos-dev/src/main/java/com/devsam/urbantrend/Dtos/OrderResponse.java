package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private String productName;
    private UUID assignedTo;
    private Long phoneNumber;
    private UUID branchId;
    private OrderStatus orderStatus;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String accountNumber;
    private BigDecimal balance;
    private String deliveryCity;
    private String deliveryNotes;
    private String invoicePdfUrl;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

}
