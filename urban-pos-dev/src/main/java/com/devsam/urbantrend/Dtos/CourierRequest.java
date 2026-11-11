package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierRequest {
    private String name;
    private String phoneNumber;
    private String vehicleInfo;
    private String notes;
    private Boolean active;
}