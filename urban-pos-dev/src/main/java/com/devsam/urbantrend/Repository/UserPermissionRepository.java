package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPermissionRepository extends JpaRepository<UserPermission, UUID> {
    List<UserPermission> findByUserId(UUID userId);
    Optional<UserPermission> findByUserIdAndPermission(UUID userId, Permission permission);
    Optional<UserPermission> findByUser_EmailAndPermission(String email, Permission permission);
    void deleteByUserId(UUID userId);
}