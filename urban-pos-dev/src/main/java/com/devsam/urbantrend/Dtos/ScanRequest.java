package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScanRequest {

    // Text read by the barcode scanner
    private String barcode;

    // Optional: the order being packed. When empty, the oldest awaiting order that needs the product is used.
    private UUID orderId;
}
