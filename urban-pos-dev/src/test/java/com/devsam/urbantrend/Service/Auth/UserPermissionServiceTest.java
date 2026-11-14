package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceTest {

    @Mock private UserPermissionRepository permissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthCache authCache;

    @Captor private ArgumentCaptor<List<UserPermission>> savedCaptor;

    private UserPermissionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserPermissionService(permissionRepository, userRepository, authCache);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("jane@example.com");
    }

    @Test
    void permissionsAddedAfterTheUserWasCreatedCanStillBeGranted() {
        // The user only has a row for one old permission; CAN_VIEW_COURIERS has none.
        UserPermission old = UserPermission.builder()
                .user(user).permission(Permission.CAN_VIEW_ORDERS).granted(true).build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(permissionRepository.findByUserId(user.getId())).thenReturn(List.of(old));

        service.updateUserPermissions(user.getId(), List.of("CAN_VIEW_COURIERS"));

        verify(permissionRepository).saveAll(savedCaptor.capture());
        List<UserPermission> saved = savedCaptor.getValue();

        assertThat(saved).hasSize(Permission.values().length);
        assertThat(saved).filteredOn(p -> p.getPermission() == Permission.CAN_VIEW_COURIERS)
                .singleElement().satisfies(p -> assertThat(p.isGranted()).isTrue());
        // The existing row is updated in place (and revoked, as it was not in the list).
        assertThat(saved).filteredOn(p -> p.getPermission() == Permission.CAN_VIEW_ORDERS)
                .singleElement().satisfies(p -> {
                    assertThat(p).isSameAs(old);
                    assertThat(p.isGranted()).isFalse();
                });
        verify(authCache).evict("jane@example.com");
    }

    @Test
    void unknownPermissionNamesAreRejectedInsteadOfIgnored() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateUserPermissions(user.getId(), List.of("CAN_FLY")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown permission: CAN_FLY");

        verify(permissionRepository, never()).saveAll(any());
    }

    @Test
    void missingListAndUnknownUserAreClientErrors() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.updateUserPermissions(user.getId(), null))
                .isInstanceOf(IllegalArgumentException.class);

        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateUserPermissions(missing, List.of()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
