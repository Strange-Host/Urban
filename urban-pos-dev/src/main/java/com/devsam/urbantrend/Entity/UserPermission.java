package com.devsam.urbantrend.Entity;


import com.devsam.urbantrend.Enum.Permission;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "user_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Permission permission;

    private boolean granted = true;
}
