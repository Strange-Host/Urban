package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.ExpenseType;
import com.devsam.urbantrend.Enum.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseRequest {

    @NotNull
    private ExpenseType expenseType;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String description;

    @NotNull
    private PaymentMethod paymentMethod;

    private String referenceCode;
}