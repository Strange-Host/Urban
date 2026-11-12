package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived cache of the user + permission lookup that every authenticated
 * request needs. Permissions are still resolved from the database rather than
 * the JWT so a grant applies without re-login; this only avoids repeating the
 * same two queries for a minute. Every path that mutates a user or its
 * permissions must call {@link #evict(String)}.
 */
@Component
@RequiredArgsConstructor
public class AuthCache {

    private static final long TTL_NANOS = TimeUnit.SECONDS.toNanos(60);

    private record Entry(UserDetails details, long expiresAt) {}

    private final UserRepository userRepository;
    private final UserPermissionRepository permissionRepository;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public Optional<UserDetails> find(String email) {
        if (email == null) return Optional.empty();

        String key = email.trim().toLowerCase();
        Entry entry = entries.get(key);

        if (entry == null) return Optional.empty();
        if (entry.expiresAt() <= System.nanoTime()) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.details());
    }

    public Optional<UserDetails> load(String email) {
        if (email == null) return Optional.empty();

        Optional<User> found = userRepository.findByEmailAndDeletedAtIsNull(email);
        if (found.isEmpty()) {
            evict(email);
            return Optional.empty();
        }

        UserDetails details = build(found.get());
        entries.put(email.trim().toLowerCase(),
                new Entry(details, System.nanoTime() + TTL_NANOS));
        return Optional.of(details);
    }

    public void evict(String email) {
        if (email == null) return;
        entries.remove(email.trim().toLowerCase());
    }

    private UserDetails build(User user) {

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        if (user.getRole() == UserRole.ADMIN) {
            Arrays.stream(Permission.values())
                    .map(p -> new SimpleGrantedAuthority(p.name()))
                    .forEach(authorities::add);
        } else {
            permissionRepository.findByUserId(user.getId())
                    .stream()
                    .filter(UserPermission::isGranted)
                    .map(p -> new SimpleGrantedAuthority(p.getPermission().name()))
                    .forEach(authorities::add);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
