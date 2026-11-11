package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private UUID customerId;
    private UUID assignedTo;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal deliveryFee;
    private String deliveryCity;
    private String deliveryNotes;
    private String invoicePdfUrl;
    private UUID createdBy;
    private List<OrderItemRequest> items;
    private OrderType orderType;
    private BigDecimal discount;
}
