package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierResponse {
    private UUID id;
    private String name;
    private String phoneNumber;
    private String vehicleInfo;
    private String notes;
    private boolean active;
    private LocalDateTime createdAt;
}