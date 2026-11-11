package com.devsam.urbantrend.Dtos;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class LeadResponse {

    private Long id;
    private String name;
    private String phoneNumber;
    private String location;
    private String status;
    private OffsetDateTime created;
    private String productName;
    private String sellingPrice;
}