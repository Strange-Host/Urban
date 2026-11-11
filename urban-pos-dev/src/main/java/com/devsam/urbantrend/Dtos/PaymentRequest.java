package com.devsam.urbantrend.Dtos;
import lombok.Data;

import java.math.BigDecimal;

import com.devsam.urbantrend.Enum.PaymentMethod;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class PaymentRequest {

    private UUID orderId;
    private PaymentMethod method;
    private BigDecimal amount;
    private String transactionRef;
    private String payerPhone;
    private String payerName;
    private OffsetDateTime paymentDate;
    private String notes;
}

