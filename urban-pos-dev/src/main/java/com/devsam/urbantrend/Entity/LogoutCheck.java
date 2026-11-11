package com.devsam.urbantrend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logout_checks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LogoutCheck {

    @Id
    @GeneratedValue
    private UUID id;

    private Boolean ordersDone;
    private String ordersReason;

    private Boolean parcelsDone;
    private String parcelsReason;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime createdAt;
}