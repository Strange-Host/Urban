package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.LogoutCheck;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.LogoutCheckRepository;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Security.Jwt.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserPermissionRepository userPermissionRepository;
    private final OtpService otpService;
    private final LogoutCheckRepository logoutCheckRepository;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Only ADMIN can create users");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()
                || request.getFirstName() == null || request.getFirstName().isBlank()
                || request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()
                || request.getRole() == null) {
            throw new IllegalArgumentException("firstName, lastName, email, phoneNumber, password and role are required");
        }

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        // ✅ Seed default permissions based on role
        seedPermissions(user);

        return new RegisterResponse("User created successfully", user.getEmail());
    }

    private void seedPermissions(User user) {
        if (user.getRole().name().equals("ADMIN")) return;

        Set<Permission> rolePermissions = user.getRole().getPermissions();

        List<UserPermission> permissions = Arrays.stream(Permission.values())
                .map(p -> UserPermission.builder()
                        .user(user)
                        .permission(p)
                        .granted(rolePermissions.contains(p))
                        .build())
                .collect(Collectors.toList());

        userPermissionRepository.saveAll(permissions);
    }

    public AuthResponse login(LoginRequest request) {

        var user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // 🔐 Send OTP
        otpService.generateAndSendOtp(user);

        return new AuthResponse(
                null,
                null,
                "OTP_SENT"
        );
    }

    private List<String> buildAuthorities(User user) {

        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + user.getRole().name());

        if (user.getRole().name().equals("ADMIN")) {
            Arrays.stream(Permission.values())
                    .map(Permission::name)
                    .forEach(authorities::add);
        } else {
            userPermissionRepository.findByUserId(user.getId())
                    .stream()
                    .filter(UserPermission::isGranted)
                    .map(p -> p.getPermission().name())
                    .forEach(authorities::add);
        }

        return authorities;
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Validate stored refresh token
        if (!refreshToken.equals(user.getRefreshToken()) ||
                user.getRefreshTokenExpiry() == null ||
                user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired or invalid");
        }

        // Build authorities the same way as login
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + user.getRole().name());

        if (user.getRole().name().equals("ADMIN")) {
            Arrays.stream(Permission.values())
                    .map(Permission::name)
                    .forEach(authorities::add);
        } else {
            userPermissionRepository.findByUserId(user.getId())
                    .stream()
                    .filter(UserPermission::isGranted)
                    .map(p -> p.getPermission().name())
                    .forEach(authorities::add);
        }

        String newAccessToken = jwtService.generateAccessToken(
                email,
                user.getRole().name(),
                authorities
        );

        return new AuthResponse(newAccessToken, refreshToken, "SUCCESS");
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAllByDeletedAtIsNull(pageable);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    public void deleteUserById(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + id + " not found")
                );

        user.setDeletedAt(LocalDateTime.now());
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void logout(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);

        user.setLastLogoutAt(LocalDateTime.now());

        userRepository.save(user);

        SecurityContextHolder.clearContext();
    }

    public AuthResponse verifyOtp(String email, String otp) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));

        boolean isValid = otpService.verifyOtp(user.getPhoneNumber(), otp);

        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        List<String> authorities = buildAuthorities(user);

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                authorities
        );

        String refreshToken = jwtService.generateRefreshToken(user.getEmail());


        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken, "SUCCESS");
    }

    public void logoutCheckService(User user, LogoutCheckRequest request) {

        // 🔐 Validation
        if (request.getOrdersDone() == null) {
            throw new RuntimeException("Orders completion status is required");
        }

        if (request.getParcelsDone() == null) {
            throw new RuntimeException("Parcels dispatch status is required");
        }

        if (Boolean.FALSE.equals(request.getOrdersDone()) &&
                (request.getOrdersReason() == null || request.getOrdersReason().isBlank())) {
            throw new RuntimeException("Provide reason for incomplete orders");
        }

        if (Boolean.FALSE.equals(request.getParcelsDone()) &&
                (request.getParcelsReason() == null || request.getParcelsReason().isBlank())) {
            throw new RuntimeException("Provide reason for undispatched parcels");
        }

        // 🧹 Clean inputs
        String ordersReason = clean(request.getOrdersReason());
        String parcelsReason = clean(request.getParcelsReason());
        String notes = clean(request.getNotes());

        // 🧾 Build entity
        LogoutCheck logoutCheck = LogoutCheck.builder()
                .ordersDone(request.getOrdersDone())
                .ordersReason(ordersReason)
                .parcelsDone(request.getParcelsDone())
                .parcelsReason(parcelsReason)
                .notes(notes)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        // 💾 Save
        logoutCheckRepository.save(logoutCheck);
    }
    private String clean(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
