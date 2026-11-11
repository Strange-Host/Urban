package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryRequest {

    private UUID productId;
    private BigDecimal quantity;
    private BigDecimal reorderLevel;
}
