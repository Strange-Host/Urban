package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCacheTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPermissionRepository permissionRepository;

    private AuthCache cache;

    @BeforeEach
    void setUp() {
        cache = new AuthCache(userRepository, permissionRepository);
    }

    private User user(UUID id, UserRole role) {
        User user = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .phoneNumber("0700000000")
                .password("encoded")
                .role(role)
                .build();
        user.setId(id);
        return user;
    }

    @Test
    void loadCachesAdminWithEveryPermission() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByEmailAndDeletedAtIsNull("jane@example.com"))
                .thenReturn(Optional.of(user(id, UserRole.ADMIN)));

        Optional<UserDetails> loaded = cache.load("jane@example.com");

        assertThat(loaded).isPresent();
        List<String> authorities = loaded.get().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(authorities).contains("ROLE_ADMIN");
        assertThat(authorities).containsAll(
                java.util.Arrays.stream(Permission.values()).map(Enum::name).toList());
        assertThat(authorities).hasSize(Permission.values().length + 1);

        // A second lookup by a differently-cased key is served from the cache.
        assertThat(cache.find("JANE@example.com")).isPresent();
        verify(userRepository, times(1)).findByEmailAndDeletedAtIsNull("jane@example.com");
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void loadKeepsOnlyGrantedPermissionsForNonAdmin() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByEmailAndDeletedAtIsNull("jane@example.com"))
                .thenReturn(Optional.of(user(id, UserRole.STAFF)));
        when(permissionRepository.findByUserId(id)).thenReturn(List.of(
                UserPermission.builder().permission(Permission.CAN_VIEW_ORDERS).granted(true).build(),
                UserPermission.builder().permission(Permission.CAN_DELETE_STAFF).granted(false).build()
        ));

        List<String> authorities = cache.load("jane@example.com").orElseThrow()
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertThat(authorities)
                .containsExactlyInAnyOrder("ROLE_STAFF", "CAN_VIEW_ORDERS");
    }

    @Test
    void missingUserReturnsEmptyWithoutQueryingPermissions() {
        when(userRepository.findByEmailAndDeletedAtIsNull("gone@example.com"))
                .thenReturn(Optional.empty());

        assertThat(cache.load("gone@example.com")).isEmpty();
        assertThat(cache.find("gone@example.com")).isEmpty();
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void evictForcesTheNextLoadToHitTheDatabase() {
        when(userRepository.findByEmailAndDeletedAtIsNull("jane@example.com"))
                .thenReturn(Optional.of(user(UUID.randomUUID(), UserRole.ADMIN)));

        cache.load("jane@example.com");
        cache.evict(" JANE@example.com ");

        assertThat(cache.find("jane@example.com")).isEmpty();

        cache.load("jane@example.com");
        verify(userRepository, times(2)).findByEmailAndDeletedAtIsNull("jane@example.com");
    }

    @Test
    void nullEmailIsToleratedEverywhere() {
        assertThat(cache.find(null)).isEmpty();
        assertThat(cache.load(null)).isEmpty();
        cache.evict(null);

        verifyNoInteractions(userRepository, permissionRepository);
    }
}
