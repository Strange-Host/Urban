package com.devsam.urbantrend.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductRequest {

    @NotBlank
    private String name;

    private String sku;

    @NotNull
    private BigDecimal sellingPrice;

    @NotNull
    private BigDecimal costPrice;

    private UUID categoryId;

    private UUID assignedStaffId;
}