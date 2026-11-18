package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.AuthSession;
import com.devsam.urbantrend.Entity.LogoutCheck;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.AuthSessionRepository;
import com.devsam.urbantrend.Repository.LogoutCheckRepository;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Security.Jwt.JwtService;
import com.devsam.urbantrend.Util.PasswordPolicy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
    private final AuthCache authCache;
    private final AuthSessionRepository authSessionRepository;

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
            throw new IllegalArgumentException("firstName, email, phoneNumber, password and role are required");
        }

        PasswordPolicy.require(request.getPassword());

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName() == null ? "" : request.getLastName().trim())
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

        var user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // 🔐 Send OTP
        otpService.generateAndSendOtp(user);

        return new AuthResponse(
                null,
                null,
                "OTP_SENT"
        );
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    // Identity shown in the UI and used for "my" screens; kept small on purpose.
    private static Map<String, Object> profileClaims(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId().toString());
        claims.put("name", (first + " " + last).trim());
        claims.put("phoneNumber", user.getPhoneNumber() == null ? "" : user.getPhoneNumber());
        return claims;
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
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtService.isTokenValid(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Each device has its own session. A refresh token is valid while its session is, so
        // logging out on one device ends only that device's session.
        UUID sessionId = jwtService.extractSessionId(refreshToken);
        if (sessionId != null) {
            AuthSession session = authSessionRepository.findById(sessionId).orElse(null);
            if (session == null || !session.getUserId().equals(user.getId())
                    || !session.isActive(LocalDateTime.now())) {
                throw new BadCredentialsException("Refresh token expired or invalid");
            }
        } else {
            // Tokens issued before sessions existed: they can only be ended by the old
            // "log out everywhere" rule, and they die on their own within the refresh lifetime.
            Date issuedAt = jwtService.extractIssuedAt(refreshToken);
            if (issuedAt == null || isRevokedByLogout(user, issuedAt)) {
                throw new BadCredentialsException("Refresh token expired or invalid");
            }
        }

        String newAccessToken = sessionId == null
                ? jwtService.generateAccessToken(email, user.getRole().name(),
                        buildAuthorities(user), profileClaims(user))
                : jwtService.generateAccessToken(email, user.getRole().name(),
                        buildAuthorities(user), profileClaims(user), sessionId);

        return new AuthResponse(newAccessToken, refreshToken, "SUCCESS");
    }

    private static boolean isRevokedByLogout(User user, Date issuedAt) {
        if (user.getLastLogoutAt() == null) {
            return false;
        }
        // Issue times only have second precision, so compare at the second the user logged out.
        Instant loggedOutAt = user.getLastLogoutAt()
                .atZone(ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS)
                .toInstant();
        return issuedAt.toInstant().isBefore(loggedOutAt);
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return getAllUsers(pageable, null);
    }

    public Page<User> getAllUsers(Pageable pageable, String query) {
        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (!hasQuery) {
            return userRepository.findAllByDeletedAtIsNull(pageable);
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return userRepository.searchActiveByPattern(pattern, pageable);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    public void deleteUserById(UUID id) {
        deleteUserById(id, null);
    }

    /**
     * Soft-deletes a staff account. An admin cannot delete their own account, and the last
     * active admin can never be deleted, otherwise nobody could manage the system any more.
     * The deleted account's email is released so it can be used for a new account.
     */
    @Transactional
    public void deleteUserById(UUID id, String actingEmail) {

        User user = userRepository.findById(id)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + id + " not found")
                );

        if (actingEmail != null && actingEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        if (user.getRole() == UserRole.ADMIN
                && userRepository.countByRoleAndDeletedAtIsNull(UserRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("The last admin account cannot be deleted");
        }

        String originalEmail = user.getEmail();

        authSessionRepository.revokeAllForUser(user.getId(), LocalDateTime.now());
        user.setDeletedAt(LocalDateTime.now());
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        user.setEmail(releasedEmail(user.getId(), originalEmail));
        userRepository.save(user);

        authCache.evict(originalEmail);
    }

    // Emails are unique across all rows, deleted ones included, so a deleted account keeps a
    // tombstone address and the real one becomes free again.
    private static String releasedEmail(UUID id, String email) {
        String tombstone = "deleted-" + id.toString().substring(0, 8) + "-" + email;
        return tombstone.length() > 255 ? tombstone.substring(0, 255) : tombstone;
    }

    /**
     * Ends one device's session. With a session id only that session is revoked; without one
     * (a token issued before sessions existed) the old behaviour applies and every older
     * refresh token for the user is revoked.
     */
    @Transactional
    public void logout(String email, UUID sessionId) {
        if (sessionId != null) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            authSessionRepository.findById(sessionId)
                    .filter(s -> s.getUserId().equals(user.getId()))
                    .ifPresent(s -> {
                        if (s.getRevokedAt() == null) {
                            s.setRevokedAt(LocalDateTime.now());
                            authSessionRepository.save(s);
                        }
                    });
            SecurityContextHolder.clearContext();
            return;
        }
        logout(email);
    }

    @Transactional
    public void logout(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);

        user.setLastLogoutAt(LocalDateTime.now());

        userRepository.save(user);

        SecurityContextHolder.clearContext();
    }

    public AuthResponse verifyOtp(String email, String otp) {

        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(email))
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired OTP"));

        boolean isValid = otpService.verifyOtp(user.getPhoneNumber(), otp);

        if (!isValid) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        List<String> authorities = buildAuthorities(user);

        AuthSession session = new AuthSession(UUID.randomUUID(), user.getId(),
                LocalDateTime.now(), LocalDateTime.now().plusDays(7), null);
        authSessionRepository.save(session);

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                authorities,
                profileClaims(user),
                session.getId()
        );

        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), session.getId());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(session.getExpiresAt());
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken, "SUCCESS");
    }

    public void logoutCheckService(User user, LogoutCheckRequest request) {

        // 🔐 Validation
        if (request.getOrdersDone() == null) {
            throw new IllegalArgumentException("Orders completion status is required");
        }

        if (request.getParcelsDone() == null) {
            throw new IllegalArgumentException("Parcels dispatch status is required");
        }

        if (Boolean.FALSE.equals(request.getOrdersDone()) &&
                (request.getOrdersReason() == null || request.getOrdersReason().isBlank())) {
            throw new IllegalArgumentException("Provide reason for incomplete orders");
        }

        if (Boolean.FALSE.equals(request.getParcelsDone()) &&
                (request.getParcelsReason() == null || request.getParcelsReason().isBlank())) {
            throw new IllegalArgumentException("Provide reason for undispatched parcels");
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
