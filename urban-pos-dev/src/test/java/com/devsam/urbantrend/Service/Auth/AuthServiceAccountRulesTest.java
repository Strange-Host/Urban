package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.RegisterRequest;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.LogoutCheckRepository;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Security.Jwt.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceAccountRulesTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserPermissionRepository userPermissionRepository;
    @Mock private OtpService otpService;
    @Mock private LogoutCheckRepository logoutCheckRepository;
    @Mock private AuthCache authCache;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService,
                userPermissionRepository, otpService, logoutCheckRepository, authCache);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "x",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private RegisterRequest registration(String password, String lastName) {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Wanjiru");
        request.setLastName(lastName);
        request.setEmail("wanjiru@example.com");
        request.setPhoneNumber("0712345678");
        request.setPassword(password);
        request.setRole(UserRole.STAFF);
        return request;
    }

    private User user(UserRole role, String email) {
        User user = User.builder()
                .firstName("Test").lastName("User").email(email)
                .phoneNumber("0700000000").password("hash").role(role).build();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void weakPasswordsAreRejected() {
        for (String weak : new String[]{"short1", "onlyletters", "12345678", "        "}) {
            assertThatThrownBy(() -> service.register(registration(weak, "Kamau")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void aStrongPasswordIsAccepted() {
        when(passwordEncoder.encode("Sunrise2026")).thenReturn("hash");

        assertThat(service.register(registration("Sunrise2026", "Kamau")).getEmail())
                .isEqualTo("wanjiru@example.com");
    }

    @Test
    void anAdminCannotDeleteTheirOwnAccount() {
        User admin = user(UserRole.ADMIN, "admin@example.com");
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.deleteUserById(admin.getId(), "ADMIN@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own account");
        assertThat(admin.getDeletedAt()).isNull();
    }

    @Test
    void theLastAdminCannotBeDeleted() {
        User admin = user(UserRole.ADMIN, "admin@example.com");
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndDeletedAtIsNull(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteUserById(admin.getId(), "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last admin");
        assertThat(admin.getDeletedAt()).isNull();
    }

    @Test
    void anotherAdminCanBeDeletedWhenOthersRemain() {
        User admin = user(UserRole.ADMIN, "second@example.com");
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndDeletedAtIsNull(UserRole.ADMIN)).thenReturn(2L);

        service.deleteUserById(admin.getId(), "admin@example.com");

        assertThat(admin.getDeletedAt()).isNotNull();
    }

    @Test
    void deletingAnAccountFreesItsEmailForReuse() {
        User staff = user(UserRole.STAFF, "staff@example.com");
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        service.deleteUserById(staff.getId(), "admin@example.com");

        assertThat(staff.getEmail()).isNotEqualTo("staff@example.com").contains("staff@example.com");
        verify(authCache).evict("staff@example.com");
    }

    @Test
    void anAlreadyDeletedAccountIsNotFound() {
        User staff = user(UserRole.STAFF, "staff@example.com");
        staff.setDeletedAt(java.time.LocalDateTime.now());
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.deleteUserById(staff.getId(), "admin@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
