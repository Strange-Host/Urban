package com.devsam.urbantrend.Security;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.email:}")
    private String adminEmail;

    @Value("${seed.admin.password:}")
    private String adminPassword;

    @Value("${seed.admin.phone:}")
    private String adminPhone;

    @Bean
    CommandLineRunner seedOwner() {
        return args -> {

            if (userRepository.existsByRole(UserRole.ADMIN)) {
                return;
            }

            if (adminEmail.isBlank() || adminPassword.isBlank() || adminPhone.isBlank()) {
                log.warn("No ADMIN user exists and SEED_ADMIN_EMAIL / SEED_ADMIN_PASSWORD / SEED_ADMIN_PHONE "
                        + "are not configured. Skipping admin seed.");
                return;
            }

            String email = adminEmail.trim().toLowerCase();

            if (userRepository.existsByEmail(email)) {
                log.warn("Cannot seed ADMIN: a user with email {} already exists", email);
                return;
            }

            User owner = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email(email)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .phoneNumber(adminPhone.trim())
                    .build();

            userRepository.save(owner);

            log.info("Seeded initial ADMIN user {}", email);
        };
    }
}
