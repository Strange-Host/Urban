package com.devsam.urbantrend.Security;

import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.StaffLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Writes a staff-log entry for every successful change made through the API. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogInterceptor implements HandlerInterceptor {

    private final StaffLogService staffLogService;
    private final UserRepository userRepository;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            int status = response.getStatus();
            if (ex != null || status < 200 || status >= 300) {
                return; // failed attempts changed nothing
            }

            ActivityLogClassifier.Entry entry = ActivityLogClassifier.classify(
                    request.getMethod(), request.getRequestURI(), request.getQueryString());
            if (entry == null) {
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return;
            }

            userRepository.findByEmail(auth.getName()).ifPresent(user ->
                    staffLogService.log(
                            user.getId().toString(),
                            entry.action(),
                            entry.entity(),
                            entry.entityId(),
                            request.getRequestURI(),
                            request.getMethod(),
                            entry.description()));
        } catch (RuntimeException e) {
            // An audit-log problem must never turn a successful action into an error.
            log.warn("Could not write staff log entry", e);
        }
    }
}
