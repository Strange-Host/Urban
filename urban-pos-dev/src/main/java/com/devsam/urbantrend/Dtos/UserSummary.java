package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;

    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return UserSummary.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public String getName() {
        String first = firstName == null ? "" : firstName;
        String last = lastName == null ? "" : lastName;
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? null : fullName;
    }
}