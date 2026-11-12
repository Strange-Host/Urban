package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final AuthCache authCache;


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

        authCache.evict(user.getEmail());
    }

    public boolean hasPermission(UUID userId, Permission permission) {
        return permissionRepository
                .findByUserIdAndPermission(userId, permission)
                .map(UserPermission::isGranted)
                .orElse(false);
    }

    /**
     * Checks a user's permission against the database instead of the authorities
     * embedded in the JWT, so a grant applies without the user logging out and
     * back in. Reads the database directly on a cache miss; the cached copy
     * lives for at most a minute and is evicted whenever permissions change.
     */
    public boolean hasPermissionByEmail(String email, Permission permission) {

        UserDetails details = authCache.find(email)
                .or(() -> authCache.load(email))
                .orElse(null);

        if (details == null) return false;

        for (GrantedAuthority authority : details.getAuthorities()) {
            if (authority.getAuthority().equals(permission.name())) return true;
        }
        return false;
    }

    public boolean hasPermissionByEmail(String email, String permission) {
        return hasPermissionByEmail(email, Permission.valueOf(permission));
    }
}
