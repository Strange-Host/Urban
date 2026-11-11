package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TopSellingProductResponse {

    private UUID productId;
    private String productName;
    private Long totalQuantity;
}
