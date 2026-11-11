package com.devsam.urbantrend.Dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String sku;
    private BigDecimal sellingPrice;
    private UUID categoryId;
    private UUID assignedStaffId;
    private String assignedStaffName;
}