package com.devsam.urbantrend.Dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductRequest {

    private String name;

    private String sku;

    private BigDecimal sellingPrice;

    private BigDecimal costPrice;

    private UUID categoryId;

    private UUID assignedStaffId;
}