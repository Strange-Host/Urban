package com.devsam.urbantrend.Controller;


import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Service.Auth.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final UserPermissionService permissionService;
    private final UserRepository userRepository;


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserPermissions(@PathVariable UUID userId) {
        List<UserPermission> permissions = permissionService.getUserPermissions(userId);
        return ResponseEntity.ok(permissions);
    }
    // All staff with their current permissions
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllStaff() {

        List<User> staff = userRepository.findByRoleNot(UserRole.ADMIN);

        List<Map<String, Object>> result = staff.stream().map(user -> {
            List<String> granted = permissionService.getUserPermissions(user.getId())
                    .stream()
                    .filter(UserPermission::isGranted)
                    .map(p -> p.getPermission().name())
                    .collect(Collectors.toList());

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId", user.getId());
            map.put("name", user.getFirstName() + " " + user.getLastName());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().name());
            map.put("grantedPermissions", granted);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // All available permissions for the UI checkboxes
    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAvailable() {
        return ResponseEntity.ok(
                Arrays.stream(Permission.values())
                        .map(Enum::name)
                        .collect(Collectors.toList())
        );
    }

    // Save permissions for a user
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePermissions(
            @PathVariable UUID userId,
            @RequestBody List<String> grantedPermissions
    ) {
        permissionService.updateUserPermissions(userId, grantedPermissions);
        return ResponseEntity.ok("Permissions updated");
    }
}
