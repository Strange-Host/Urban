package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.AuthResponse;
import com.devsam.urbantrend.Dtos.LoginRequest;
import com.devsam.urbantrend.Dtos.LogoutCheckRequest;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LogoutCheckRepository;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Security.Jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserPermissionRepository userPermissionRepository;
    @Mock private OtpService otpService;
    @Mock private LogoutCheckRepository logoutCheckRepository;
    @Mock private AuthCache authCache;

    @Captor private ArgumentCaptor<Map<String, Object>> profileCaptor;

    private AuthService service;
    private User jane;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService,
                userPermissionRepository, otpService, logoutCheckRepository, authCache);

        jane = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .phoneNumber("0712345678")
                .password("hash")
                .role(UserRole.STAFF)
                .build();
        jane.setId(UUID.randomUUID());
    }

    @Test
    void loginIgnoresEmailCaseAndSurroundingSpaces() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("jane@example.com"))
                .thenReturn(Optional.of(jane));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("  Jane@Example.COM ");
        request.setPassword("secret");

        AuthResponse response = service.login(request);

        assertThat(response.getMessage()).isEqualTo("OTP_SENT");
        verify(otpService).generateAndSendOtp(jane);
    }

    @Test
    void unknownEmailGetsTheSameGenericError() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("nobody@example.com"))
                .thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("Nobody@Example.com");
        request.setPassword("secret");

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void otpVerificationAlsoIgnoresEmailCase() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("jane@example.com"))
                .thenReturn(Optional.of(jane));
        when(otpService.verifyOtp("0712345678", "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(eq("jane@example.com"), eq("STAFF"), anyList(), anyMap()))
                .thenReturn("access");
        when(jwtService.generateRefreshToken("jane@example.com")).thenReturn("refresh");

        AuthResponse response = service.verifyOtp(" JANE@example.com", "123456");

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void incompleteLogoutChecksAreClientErrorsNotServerErrors() {
        // These used to be plain RuntimeExceptions, i.e. a 500 with a generic message.
        LogoutCheckRequest missingReason = new LogoutCheckRequest();
        missingReason.setOrdersDone(false);
        missingReason.setParcelsDone(true);

        assertThatThrownBy(() -> service.logoutCheckService(jane, missingReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provide reason for incomplete orders");

        assertThatThrownBy(() -> service.logoutCheckService(jane, new LogoutCheckRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Orders completion status is required");
    }

    @Test
    void accessTokenCarriesIdNameAndPhone() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("jane@example.com"))
                .thenReturn(Optional.of(jane));
        when(otpService.verifyOtp("0712345678", "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(eq("jane@example.com"), eq("STAFF"), anyList(), anyMap()))
                .thenReturn("access");

        service.verifyOtp("jane@example.com", "123456");

        verify(jwtService).generateAccessToken(
                eq("jane@example.com"), eq("STAFF"), anyList(), profileCaptor.capture());
        assertThat(profileCaptor.getValue())
                .containsEntry("id", jane.getId().toString())
                .containsEntry("name", "Jane Doe")
                .containsEntry("phoneNumber", "0712345678");
    }

    @Test
    void refreshedTokensKeepTheIdentityClaims() {
        when(jwtService.isTokenValid("refresh")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh")).thenReturn(true);
        when(jwtService.extractEmail("refresh")).thenReturn("jane@example.com");
        jane.setRefreshToken("refresh");
        jane.setRefreshTokenExpiry(java.time.LocalDateTime.now().plusDays(1));
        when(userRepository.findByEmailAndDeletedAtIsNull("jane@example.com"))
                .thenReturn(Optional.of(jane));
        when(userPermissionRepository.findByUserId(jane.getId())).thenReturn(List.of());
        when(jwtService.generateAccessToken(eq("jane@example.com"), eq("STAFF"), anyList(), anyMap()))
                .thenReturn("new-access");

        AuthResponse response = service.refresh("refresh");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        verify(jwtService).generateAccessToken(
                eq("jane@example.com"), eq("STAFF"), anyList(), profileCaptor.capture());
        assertThat(profileCaptor.getValue()).containsKey("id");
    }
}
