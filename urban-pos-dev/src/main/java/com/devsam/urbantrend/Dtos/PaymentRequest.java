package com.devsam.urbantrend.Dtos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

import com.devsam.urbantrend.Enum.PaymentMethod;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class PaymentRequest {

    private UUID orderId;

    @NotNull
    private PaymentMethod method;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String transactionRef;
    private String payerPhone;
    private String payerName;
    private OffsetDateTime paymentDate;
    private String notes;
}