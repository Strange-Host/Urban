package com.devsam.urbantrend.Entity;

import com.devsam.urbantrend.Enum.RequisitionStatus;
import com.devsam.urbantrend.Enum.RequisitionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "requisitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Requisition extends BaseEntity{

    @Enumerated(EnumType.STRING)
    @Column(name="requisition_type", nullable=false)
    private RequisitionType requisitionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private RequisitionStatus status = RequisitionStatus.PENDING;

    @Column(nullable=false, length=150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name="amount_requested", nullable=false)
    private BigDecimal amountRequested;

    @ManyToOne(optional=false)
    @JoinColumn(name="requested_by")
    private User requestedBy;

    @ManyToOne
    @JoinColumn(name="approved_by")
    private User approvedBy;

    private OffsetDateTime approvedAt;

    @ManyToOne
    @JoinColumn(name="rejected_by")
    private User rejectedBy;

    private OffsetDateTime rejectedAt;

    @Column(columnDefinition="TEXT")
    private String rejectionReason;

    @ManyToOne
    @JoinColumn(name="paid_by")
    private User paidBy;

    private OffsetDateTime paidAt;

}

