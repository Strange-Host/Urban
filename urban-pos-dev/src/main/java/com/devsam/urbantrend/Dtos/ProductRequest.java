package com.devsam.urbantrend.Dtos;

import jakarta.validation.constraints.DecimalMin;
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
    @DecimalMin("0")
    private BigDecimal sellingPrice;

    // Required when creating a product (checked in the service). On update only an admin
    // may change it; other roles never see the cost, so they cannot send a correct value.
    @DecimalMin("0")
    private BigDecimal costPrice;

    private UUID categoryId;

    private UUID assignedStaffId;
}