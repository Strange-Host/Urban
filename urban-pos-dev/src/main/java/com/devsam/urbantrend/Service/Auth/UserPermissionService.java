package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserPermissionRepository permissionRepository;
    private final UserRepository userRepository;


    public List<UserPermission> getUserPermissions(UUID userId) {
        return permissionRepository.findByUserId(userId);
    }

    @Transactional
    public void updateUserPermissions(UUID userId, List<String> grantedPermissions) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Update existing rows instead of delete + re-insert
        List<UserPermission> existing = permissionRepository.findByUserId(user.getId());

        if (existing.isEmpty()) {
            // First time — create all permissions from scratch
            List<UserPermission> permissions = Arrays.stream(Permission.values())
                    .map(p -> UserPermission.builder()
                            .user(user)
                            .permission(p)
                            .granted(grantedPermissions.contains(p.name()))
                            .build())
                    .collect(Collectors.toList());
            permissionRepository.saveAll(permissions);
        } else {
            // Already exist — just update the granted flag
            existing.forEach(p ->
                    p.setGranted(grantedPermissions.contains(p.getPermission().name()))
            );
            permissionRepository.saveAll(existing);
        }
    }

    public boolean hasPermission(UUID userId, Permission permission) {
        return permissionRepository
                .findByUserIdAndPermission(userId, permission)
                .map(UserPermission::isGranted)
                .orElse(false);
    }

    /**
     * Checks a user's permission in real-time against the database, bypassing
     * any cached authorities from the JWT/authentication context. This ensures
     * newly granted permissions take effect immediately without requiring the
     * user to log out and back in.
     */
    public boolean hasPermissionByEmail(String email, Permission permission) {
        // Check if user is ADMIN (admins get all permissions)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // For non-admins, check explicit permissions in database
        return permissionRepository
                .findByUser_EmailAndPermission(email, permission)
                .map(UserPermission::isGranted)
                .orElse(false);
    }

    public boolean hasPermissionByEmail(String email, String permission) {
        return hasPermissionByEmail(email, Permission.valueOf(permission));
    }
}