package com.devsam.urbantrend.Dtos;

import java.util.UUID;

public record OrderSmsDTO(
        UUID orderId,
        String customerName,
        String customerPhone,
        String productName
) {}