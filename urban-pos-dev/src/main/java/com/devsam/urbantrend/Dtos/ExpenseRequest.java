package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.ExpenseType;
import com.devsam.urbantrend.Enum.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseRequest {
    private ExpenseType expenseType;
    private BigDecimal amount;
    private String description;
    private PaymentMethod paymentMethod;
    private String referenceCode;
}
