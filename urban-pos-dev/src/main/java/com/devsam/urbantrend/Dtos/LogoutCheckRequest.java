package com.devsam.urbantrend.Dtos;


import lombok.Data;

@Data
public class LogoutCheckRequest {
    private Boolean ordersDone;
    private String ordersReason;

    private Boolean parcelsDone;
    private String parcelsReason;

    private String notes;
}