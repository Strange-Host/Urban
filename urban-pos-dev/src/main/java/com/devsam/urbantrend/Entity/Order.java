package com.devsam.urbantrend.Entity;

import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.security.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Data
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column (name = "courier_id")
    private UUID courierId;
    @Column (name = "courier_assigned_at")
    private LocalDateTime courierAssignedAt;

    @Column(name = "discount")
    private BigDecimal discount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.AWAITING_DISPATCH;

    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal balance= BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_fee", precision = 14, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "delivery_notes", columnDefinition = "text")
    private String deliveryNotes;

    @Column(name = "invoice_pdf_url", columnDefinition = "text")
    private String invoicePdfUrl;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

}
