package com.devsam.urbantrend.Entity;


import com.devsam.urbantrend.Roles.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends BaseEntity {
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @JsonIgnore
    @Column(name = "refresh_token")
    private String refreshToken;


    @JsonIgnore
    @Column(name = "refresh_token_expiry")
    private LocalDateTime refreshTokenExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_logout_at")
    private LocalDateTime lastLogoutAt;
}
