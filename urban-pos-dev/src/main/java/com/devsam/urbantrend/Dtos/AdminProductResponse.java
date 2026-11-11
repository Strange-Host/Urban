package com.devsam.urbantrend.Dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AdminProductResponse {
    private UUID id;
    private String name;
    private String sku;
    private BigDecimal sellingPrice;
    private BigDecimal costPrice;
    private UUID categoryId;
    private UUID assignedStaffId;
    private String assignedStaffName;
}