package com.devsam.urbantrend.support;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Entity.UserPermission;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Repository.UserPermissionRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Security.Jwt.JwtService;
import com.devsam.urbantrend.Service.Auth.AuthCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Boots the full application against the H2 test profile and talks to it over
 * real HTTP, so the security filter chain, method authorization and JPA all run
 * exactly as they do in production. Seeds are committed, so tests use unique
 * emails and ids rather than flushing the database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @LocalServerPort
    protected int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserPermissionRepository userPermissionRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected AuthCache authCache;

    protected User createUser(UserRole role, Permission... granted) {

        long n = SEQUENCE.incrementAndGet();
        String email = role.name().toLowerCase() + "-" + n + "@integration.urban.test";

        User user = userRepository.save(User.builder()
                .firstName("Test")
                .lastName(role.name())
                .email(email)
                .phoneNumber("2547000" + String.format("%05d", n))
                .role(role)
                .password(passwordEncoder.encode("Password123!"))
                .build());

        if (role != UserRole.ADMIN) {
            List<Permission> grantedList = Arrays.asList(granted);
            userPermissionRepository.saveAll(Arrays.stream(Permission.values())
                    .map(permission -> UserPermission.builder()
                            .user(user)
                            .permission(permission)
                            .granted(grantedList.contains(permission))
                            .build())
                    .toList());
        }

        authCache.evict(email);
        return user;
    }

    protected String bearer(User user) {
        authCache.evict(user.getEmail());
        String token = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                List.of("ROLE_" + user.getRole().name())
        );
        return "Bearer " + token;
    }

    protected HttpResponse<String> get(String path, String bearer) {
        return send(HttpRequest.newBuilder(uri(path)).GET(), bearer);
    }

    protected HttpResponse<String> postJson(String path, String body) {
        return postJson(path, body, null);
    }

    protected HttpResponse<String> postJson(String path, String body, String bearer) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return send(request, bearer);
    }

    private HttpResponse<String> send(HttpRequest.Builder request, String bearer) {
        if (bearer != null) {
            request.header("Authorization", bearer);
        }

        try {
            return http.send(request.timeout(Duration.ofSeconds(20)).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("HTTP request failed", e);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
