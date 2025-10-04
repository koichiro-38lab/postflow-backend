package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestDataConfig {

    @Bean
    public org.springframework.boot.ApplicationRunner testDataRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                // Admin ユーザー
                User admin = User.builder()
                        .email("admin@example.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .displayName("Admin User")
                        .role(User.Role.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .emailVerified(true)
                        .build();
                userRepository.save(admin);

                // Editor ユーザー
                User editor = User.builder()
                        .email("editor@example.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .displayName("Editor User")
                        .role(User.Role.EDITOR) // ← これが ADMIN/EDITOR であることを確認
                        .status(UserStatus.ACTIVE)
                        .emailVerified(true)
                        .build();
                userRepository.save(editor);

                // Author ユーザー
                User author = User.builder()
                        .email("author@example.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .displayName("Author User")
                        .role(User.Role.AUTHOR)
                        .status(UserStatus.ACTIVE)
                        .emailVerified(true)
                        .build();
                userRepository.save(author);
            }
        };
    }
}
