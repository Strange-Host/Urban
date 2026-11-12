package com.devsam.urbantrend.Security.Jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 120_000L);
    }

    @Test
    void accessTokenCarriesSubjectRoleAndAuthorities() {
        String token = jwtService.generateAccessToken(
                "jane@example.com", "ADMIN", List.of("ROLE_ADMIN", "CAN_VIEW_ORDERS"));

        assertThat(jwtService.extractEmail(token)).isEqualTo("jane@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        Object authorities = jwtService.extractClaim(token, claims -> claims.get("authorities"));
        assertThat((List<Object>) authorities).containsExactly("ROLE_ADMIN", "CAN_VIEW_ORDERS");
        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void refreshTokenIsNotAcceptedAsAnAccessToken() {
        String token = jwtService.generateRefreshToken("jane@example.com");

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void expiredTokenIsInvalid() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1_000L);

        String token = jwtService.generateAccessToken("jane@example.com", "STAFF", List.of());

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void malformedTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-token")).isFalse();
        assertThat(jwtService.isAccessToken("not-a-token")).isFalse();
        assertThat(jwtService.isRefreshToken("not-a-token")).isFalse();
    }

    @Test
    void weakOrMissingSecretIsRejected() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "short");

        assertThatThrownBy(() -> jwtService.generateAccessToken("jane@example.com", "ADMIN", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");

        ReflectionTestUtils.setField(jwtService, "secretKey", "   ");

        assertThatThrownBy(() -> jwtService.generateRefreshToken("jane@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret is not configured");
    }
}
