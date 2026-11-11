package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Entity.Requisition;
import com.devsam.urbantrend.Enum.RequisitionStatus;
import com.devsam.urbantrend.Enum.RequisitionType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class RequisitionResponse {
    private UUID id;
    private RequisitionType requisitionType;
    private RequisitionStatus status;
    private String title;
    private String description;
    private BigDecimal amountRequested;
    private UUID requestedBy;
    private OffsetDateTime createdAt;


}

