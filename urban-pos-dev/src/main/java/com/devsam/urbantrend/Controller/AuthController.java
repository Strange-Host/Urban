package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Service.Auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

//    get all users

    @GetMapping("/staffs")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Page<User> findAllUsers (Pageable pageable, @AuthenticationPrincipal UserDetails userDetails){
        String email = userDetails.getUsername();
        userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(()-> new RuntimeException("User not found"));
        return authService.getAllUsers(pageable);
    }

    @GetMapping("/user/{id}")
    @PreAuthorize("isAuthenticated()")
    public User getCustomerById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        User current = getUser(userDetails);

        boolean elevated = current.getRole() == UserRole.ADMIN || current.getRole() == UserRole.MANAGER;

        if (!elevated && !current.getId().equals(id)) {
            throw new AccessDeniedException("Not allowed to view this user");
        }

        return authService.getUserById(id);
    }

    @DeleteMapping("/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable UUID id) {
        authService.deleteUserById(id);
    }


    @PostMapping("/logout-check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logoutCheck(
            @RequestBody LogoutCheckRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        User user = getUser(userDetails);

        authService.logoutCheckService(user, request);

        return ResponseEntity.ok().build();
    }
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(200).body("Session already cleared");
        }

        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok("Logged out successfully");
    }
    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
