package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductLookupResponse {

    private UUID id;
    private String name;
    private String sku;
    private BigDecimal sellingPrice;

    public static ProductLookupResponse from(Product product) {
        return ProductLookupResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .sellingPrice(product.getSellingPrice())
                .build();
    }
}