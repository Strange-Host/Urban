package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Entity.Expense;
import com.devsam.urbantrend.Enum.ExpenseType;
import com.devsam.urbantrend.Enum.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ExpenseResponse {

    private UUID id;
    private ExpenseType expenseType;
    private BigDecimal amount;
    private String description;
    private PaymentMethod paymentMethod;
    private String referenceCode;
    private UserSummary createdBy;
    private UUID requisitionId;
    private LocalDateTime createdAt;

    public static ExpenseResponse from(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .expenseType(expense.getExpenseType())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .paymentMethod(expense.getPaymentMethod())
                .referenceCode(expense.getReferenceCode())
                .createdBy(UserSummary.from(expense.getCreatedBy()))
                .requisitionId(expense.getRequisition() != null ? expense.getRequisition().getId() : null)
                .createdAt(expense.getCreatedAt())
                .build();
    }
}