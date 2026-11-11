package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScanResponse {

    private String serial;
    private String productName;

    private UUID orderId;
    private String accountNumber;
    private OrderStatus orderStatus;

    private long unitsScanned;
    private long unitsRequired;

    // true when this scan completed the order and it was moved to DISPATCHED_*
    private boolean orderDispatched;

    private String message;
}
