package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestDataConfig {

    @Bean
    public org.springframework.boot.ApplicationRunner testDataRunner(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            users.findByEmail("test@example.com").orElseGet(() -> {
                User u = User.builder()
                        .email("test@example.com")
                        .passwordHash(encoder.encode("password123"))
                        .role(User.Role.ADMIN)
                        .build();
                return users.save(u);
            });
            users.findByEmail("author@example.com").orElseGet(() -> {
                User u = User.builder()
                        .email("author@example.com")
                        .passwordHash(encoder.encode("password123"))
                        .role(User.Role.AUTHOR)
                        .build();
                return users.save(u);
            });
        };
    }
}
