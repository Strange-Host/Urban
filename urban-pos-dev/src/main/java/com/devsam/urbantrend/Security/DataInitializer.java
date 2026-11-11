package com.devsam.urbantrend.Security;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class DataInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Bean
    CommandLineRunner seedOwner() {
        return args -> {

            boolean ownerExists = userRepository.existsByRole(UserRole.ADMIN);

            if (!ownerExists) {

                User owner = User.builder()
                        .firstName("Martha")
                        .lastName("Kamene")
                        .email("marthawrites24@gmail.com")
                        .password(passwordEncoder.encode("Password@123"))
                        .role(UserRole.ADMIN)
                        .phoneNumber("0728664772")
                        .refreshToken("INITIAL_SEED")
                        .build();

                userRepository.save(owner);

                System.out.println("✅ OWNER user seeded successfully");
            }
        };
    }
}
