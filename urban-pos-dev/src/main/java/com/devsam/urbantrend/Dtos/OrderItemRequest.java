package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor

public class OrderItemRequest {
    private UUID productId;
    private BigDecimal quantity;
}
