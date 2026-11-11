package com.devsam.urbantrend.Dtos;
import java.util.UUID;

public interface TopSellingProductProjection {

    UUID getProductId();
    String getProductName();
    Long getTotalQuantity();
}