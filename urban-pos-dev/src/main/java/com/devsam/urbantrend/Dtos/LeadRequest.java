package com.devsam.urbantrend.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LeadRequest {

    private Integer formId;
    private String formName;

    private String name;
    private String location;

    private String first;
    private String last;

    private String deliveryCounty;
    private String deliveryLocation;

    @JsonProperty("phoneNumber")
    private String phone;

    @JsonProperty("altPhoneNumber")
    private String altPhone;

    private String extraDetails;
    private Long wpEntryId;
}
