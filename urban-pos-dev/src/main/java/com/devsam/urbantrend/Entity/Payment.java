package com.devsam.urbantrend.Entity;
import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "KES";

    @Column(name = "transaction_ref", length = 100, unique = true)
    private String transactionRef;

    @Column(name = "payer_phone", length = 30)
    private String payerPhone;

    @Column(name = "payer_name", length = 140)
    private String payerName;

    @Column(name = "payment_date", nullable = false)
    private OffsetDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "reconciled_by")
    private UUID reconciledBy;

    @Column(name = "reconciled_at")
    private OffsetDateTime reconciledAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_matched")
    private boolean matched = false;

    @Column(name = "is_suspicious")
    private boolean suspicious = false;
}

