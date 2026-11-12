package com.devsam.urbantrend.Entity;

import com.devsam.urbantrend.Enum.ExpenseType;
import com.devsam.urbantrend.Enum.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name="expense_type", nullable=false)
    private ExpenseType expenseType;

    @Column(nullable=false)
    private BigDecimal amount;

    @Column(columnDefinition="TEXT")
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(name="payment_method", nullable=false)
    private PaymentMethod paymentMethod;

    private String referenceCode;

    @ManyToOne(optional=false)
    @JoinColumn(name="created_by")
    private User createdBy;

    @OneToOne
    @JoinColumn(name="requisition_id")
    private Requisition requisition;
}

