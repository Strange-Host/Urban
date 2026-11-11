package com.devsam.urbantrend.Dtos;

import lombok.Data;

import java.util.Map;

@Data
public class ManualLeadRequest {

    private String name;
    private String phoneNumber;
    private String alternativePhoneNumber;
    private String location;
    private String formName;
    private String additionalInfo;
    private String notes;
    private String source;
    private Map<String, Object> productOptions;
}