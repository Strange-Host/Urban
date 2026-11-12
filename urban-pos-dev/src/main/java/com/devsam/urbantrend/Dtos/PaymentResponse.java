package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID orderId;

    private PaymentMethod method;
    private BigDecimal amount;
    private String currency;

    private String transactionRef;
    private String payerPhone;
    private String payerName;
    private String notes;
    private OffsetDateTime paymentDate;

    private PaymentStatus status;
    private boolean matched;
    private boolean suspicious;

    private LocalDateTime createdAt;
}

