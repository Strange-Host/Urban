package com.devsam.urbantrend.Security;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.StaffLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityLogInterceptorTest {

    @Mock private StaffLogService staffLogService;
    @Mock private UserRepository userRepository;

    private ActivityLogInterceptor interceptor;
    private User user;

    @BeforeEach
    void setUp() {
        interceptor = new ActivityLogInterceptor(staffLogService, userRepository);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("jane@example.com");
        lenient().when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("jane@example.com", "x",
                        AuthorityUtils.createAuthorityList("CAN_CREATE_ORDERS")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse respond(int status) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        return response;
    }

    @Test
    void aSuccessfulChangeIsLoggedForTheSignedInStaffMember() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");

        interceptor.afterCompletion(request, respond(201), new Object(), null);

        verify(staffLogService).log(eq(user.getId().toString()), eq("CREATE"), eq("ORDER"),
                any(), eq("/api/v1/orders"), eq("POST"), any());
    }

    @Test
    void failedRequestsAreNotLogged() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");

        interceptor.afterCompletion(request, respond(400), new Object(), null);
        interceptor.afterCompletion(request, respond(201), new Object(), new RuntimeException("boom"));

        verify(staffLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void readsAreNotLogged() {
        interceptor.afterCompletion(new MockHttpServletRequest("GET", "/api/v1/orders"), respond(200), new Object(), null);

        verify(staffLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void requestsWithoutASignedInUserAreNotLogged() {
        SecurityContextHolder.clearContext();

        interceptor.afterCompletion(new MockHttpServletRequest("POST", "/api/v1/orders"), respond(201), new Object(), null);

        verify(staffLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void aLoggingFailureNeverBreaksTheRequest() {
        doThrow(new IllegalStateException("db down")).when(staffLogService)
                .log(any(), any(), any(), any(), any(), any(), any());

        interceptor.afterCompletion(new MockHttpServletRequest("POST", "/api/v1/orders"), respond(201), new Object(), null);
        // reaching here without an exception is the assertion
    }
}
