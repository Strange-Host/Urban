package com.devsam.urbantrend.Dtos;

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
public class ConvertLeadRequest {
    private UUID productId;
    private Integer quantity;
    private String productName;
    private String formName;
    private String deliveryCity;
    private BigDecimal deliveryFee;
    private BigDecimal discount;
    private UUID courierId;
    private List<OrderItemRequest> items;
}