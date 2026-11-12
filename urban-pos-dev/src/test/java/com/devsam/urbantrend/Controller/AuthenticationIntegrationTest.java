package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationIntegrationTest extends IntegrationTestBase {

    @Test
    void loginWithUnknownEmailIsRejected() {
        HttpResponse<String> response = postJson("/api/v1/auth/login", """
                {"email":"missing-user@integration.urban.test","password":"Password123!"}
                """);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        User user = createUser(UserRole.MANAGER);

        HttpResponse<String> response = postJson("/api/v1/auth/login", """
                {"email":"%s","password":"definitely-not-it"}
                """.formatted(user.getEmail()));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void loginWithValidCredentialsRequestsOtp() {
        // A blank phone number makes MessagingService skip the SMS provider,
        // so the test never reaches the external gateway.
        User user = userRepository.save(User.builder()
                .firstName("Otp")
                .lastName("Tester")
                .email("otp-login@integration.urban.test")
                .phoneNumber("")
                .role(UserRole.MANAGER)
                .password(passwordEncoder.encode("Password123!"))
                .build());
        authCache.evict(user.getEmail());

        HttpResponse<String> response = postJson("/api/v1/auth/login", """
                {"email":"%s","password":"Password123!"}
                """.formatted(user.getEmail()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("OTP_SENT");
        assertThat(response.body()).doesNotContain("accessToken\":\"ey");
    }

    @Test
    void loginRejectsMalformedEmail() {
        HttpResponse<String> response = postJson("/api/v1/auth/login", """
                {"email":"not-an-email","password":"Password123!"}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void refreshRejectsAccessToken() {
        User user = createUser(UserRole.MANAGER, Permission.CAN_VIEW_ORDERS);
        String accessToken = bearer(user).substring("Bearer ".length());

        HttpResponse<String> response = postJson("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(accessToken));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void registerIsForbiddenForStaff() {
        User staff = createUser(UserRole.MANAGER);

        HttpResponse<String> response = postJson("/api/v1/auth/register", """
                {"firstName":"New","lastName":"User","email":"new-user@integration.urban.test",
                 "phoneNumber":"254700000001","password":"Password123!","role":"CASHIER"}
                """, bearer(staff));

        assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
    }

    @Test
    void registerIsUnauthorizedWithoutToken() {
        HttpResponse<String> response = postJson("/api/v1/auth/register", """
                {"firstName":"New","lastName":"User","email":"new-user@integration.urban.test",
                 "phoneNumber":"254700000001","password":"Password123!","role":"CASHIER"}
                """);

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
