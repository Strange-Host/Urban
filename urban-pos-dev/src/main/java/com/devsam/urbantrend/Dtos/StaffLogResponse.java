package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffLogResponse {
    private String id;
    private String staffId;
    private String staffFirstName;
    private String action;
    private String entity;
    private String entityId;
    private String endpoint;
    private String method;
    private LocalDateTime timestamp;
    private String description;

}