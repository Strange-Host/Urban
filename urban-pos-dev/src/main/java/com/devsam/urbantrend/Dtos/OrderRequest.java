package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    /**
     * Either an existing customer, or the customer's details below, which are matched or
     * created in the same transaction as the order.
     */
    private UUID customerId;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerCity;

    private UUID assignedTo;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal deliveryFee;
    private String deliveryCity;
    private String deliveryNotes;
    private String invoicePdfUrl;
    private UUID createdBy;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @NotNull
    private OrderType orderType;

    private BigDecimal discount;

    /**
     * Required for COUNTER sales (must add up to the sale total); not allowed for ONLINE
     * orders, which are paid after creation.
     */
    @Valid
    private List<OrderPaymentRequest> payments;
}