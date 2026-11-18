package com.devsam.urbantrend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** One signed-in device. Both of its tokens carry this row's id in the "sid" claim. */
@Entity
@Table(name = "auth_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
