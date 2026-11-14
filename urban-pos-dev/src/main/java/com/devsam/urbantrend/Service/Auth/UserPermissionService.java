package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (grantedPermissions == null) {
            throw new IllegalArgumentException("A list of permissions is required");
        }

        // A typo or an outdated client must not silently grant nothing.
        for (String name : grantedPermissions) {
            try {
                Permission.valueOf(name);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException("Unknown permission: " + name);
            }
        }

        Map<Permission, UserPermission> existing = permissionRepository.findByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(UserPermission::getPermission, p -> p, (a, b) -> a));

        // Every permission gets a row. Users created before a permission was added have no
        // row for it, and updating only existing rows meant an admin could never grant it.
        List<UserPermission> toSave = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            boolean granted = grantedPermissions.contains(permission.name());

            UserPermission row = existing.get(permission);
            if (row == null) {
                row = UserPermission.builder()
                        .user(user)
                        .permission(permission)
                        .granted(granted)
                        .build();
            } else {
                row.setGranted(granted);
            }
            toSave.add(row);
        }
        permissionRepository.saveAll(toSave);

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
