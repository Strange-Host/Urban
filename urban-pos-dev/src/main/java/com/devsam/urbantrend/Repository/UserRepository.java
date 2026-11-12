package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Roles.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByRole(UserRole userRole);

    boolean existsByEmail(String email);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    List<User> findAllByDeletedAtIsNullAndRoleNot(UserRole role);

    List<User> findByRole(UserRole role);

    List<User> findByRoleNot(UserRole userRole);
}
