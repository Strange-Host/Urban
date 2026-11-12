package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Entity.Requisition;
import com.devsam.urbantrend.Enum.RequisitionStatus;
import com.devsam.urbantrend.Enum.RequisitionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class RequisitionResponse {

    private UUID id;
    private RequisitionType requisitionType;
    private RequisitionStatus status;
    private String title;
    private String description;
    private BigDecimal amountRequested;
    private UserSummary requestedBy;
    private UserSummary approvedBy;
    private UserSummary rejectedBy;
    private UserSummary paidBy;
    private OffsetDateTime approvedAt;
    private OffsetDateTime rejectedAt;
    private OffsetDateTime paidAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RequisitionResponse from(Requisition requisition) {
        return RequisitionResponse.builder()
                .id(requisition.getId())
                .requisitionType(requisition.getRequisitionType())
                .status(requisition.getStatus())
                .title(requisition.getTitle())
                .description(requisition.getDescription())
                .amountRequested(requisition.getAmountRequested())
                .requestedBy(UserSummary.from(requisition.getRequestedBy()))
                .approvedBy(UserSummary.from(requisition.getApprovedBy()))
                .rejectedBy(UserSummary.from(requisition.getRejectedBy()))
                .paidBy(UserSummary.from(requisition.getPaidBy()))
                .approvedAt(requisition.getApprovedAt())
                .rejectedAt(requisition.getRejectedAt())
                .paidAt(requisition.getPaidAt())
                .rejectionReason(requisition.getRejectionReason())
                .createdAt(requisition.getCreatedAt())
                .updatedAt(requisition.getUpdatedAt())
                .build();
    }
}