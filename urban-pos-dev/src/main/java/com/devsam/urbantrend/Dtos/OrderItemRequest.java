package com.devsam.urbantrend.Dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemRequest {

    @NotNull
    private UUID productId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;
}