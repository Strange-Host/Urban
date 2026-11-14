package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** One tender of a counter sale. A sale can be split across several (cash + M-Pesa, ...). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentRequest {

    @NotNull
    private PaymentMethod method;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    /** M-Pesa / bank code. Optional for cash. */
    private String transactionRef;

    private String payerPhone;
}
