package com.devsam.urbantrend.Errors;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertProblem(ResponseEntity<ProblemDetail> response, HttpStatus status, String detail) {
        assertThat(response.getStatusCode().value()).isEqualTo(status.value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo(detail);
    }

    @Test
    void notFoundBadRequestAndConflictKeepTheirMessages() {
        assertProblem(handler.handleNotFound(new ResourceNotFoundException("Lead not found")),
                HttpStatus.NOT_FOUND, "Lead not found");
        assertProblem(handler.handleBadRequest(new IllegalArgumentException("Provide reason for incomplete orders")),
                HttpStatus.BAD_REQUEST, "Provide reason for incomplete orders");
        assertProblem(handler.handleConflict(new IllegalStateException("Lead already converted")),
                HttpStatus.CONFLICT, "Lead already converted");
    }

    @Test
    void wrongCredentialsExplainThemselvesButOtherAuthFailuresStayGeneric() {
        assertProblem(handler.handleAuthentication(new BadCredentialsException("Invalid email or password")),
                HttpStatus.UNAUTHORIZED, "Invalid email or password");
        assertProblem(handler.handleAuthentication(new InsufficientAuthenticationException("secret internals")),
                HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    @Test
    void lockTimeoutsAskTheUserToRetryInsteadOfFailingWith500() {
        assertProblem(handler.handleLockFailure(new CannotAcquireLockException("lock timeout")),
                HttpStatus.CONFLICT, "Another update to this record is in progress. Please try again.");
    }

    @Test
    void parameterConstraintViolationsAreBadRequests() {
        assertProblem(handler.handleConstraintViolation(new ConstraintViolationException(Set.of())),
                HttpStatus.BAD_REQUEST, "Validation failed");
    }

    @Test
    void unexpectedErrorsNeverLeakInternalDetails() {
        assertProblem(handler.handleGeneric(new RuntimeException("password=hunter2 at SELECT ...")),
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
