package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Entity.Lead;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LeadResponse {

    private Long id;
    private String name;
    private String phoneNumber;
    private String alternativePhoneNumber;
    private String location;
    private String notes;
    private String additionalInfo;
    private Integer formId;
    private String formName;
    private String productOptions;
    private String status;
    private LocalDateTime created;
    private LocalDateTime followUpAt;
    private UUID convertedOrderId;
    private String productName;
    private String sellingPrice;

    public static LeadResponse from(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .name(lead.getName())
                .phoneNumber(lead.getPhoneNumber())
                .alternativePhoneNumber(lead.getAlternativePhoneNumber())
                .location(lead.getLocation())
                .notes(lead.getNotes())
                .additionalInfo(lead.getAdditionalInfo())
                .formId(lead.getFormId())
                .formName(lead.getFormName())
                .productOptions(lead.getProductOptions())
                .status(lead.getStatus())
                .created(lead.getCreated())
                .followUpAt(lead.getFollowUpAt())
                .convertedOrderId(lead.getConvertedOrderId())
                .build();
    }
}