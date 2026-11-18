package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Roles.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String firstName;

    /** Optional: some people have a single name. Stored as an empty string when absent. */
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String password;

    @NotNull
    private UserRole role;
}