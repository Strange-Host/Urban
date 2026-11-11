package com.devsam.urbantrend.Dtos;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String email;
    private String otp;
}
