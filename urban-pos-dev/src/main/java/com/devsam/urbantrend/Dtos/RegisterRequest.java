package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Roles.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String password;
    private UserRole role;
}