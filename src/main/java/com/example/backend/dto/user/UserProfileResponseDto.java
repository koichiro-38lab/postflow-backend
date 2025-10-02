package com.example.backend.dto.user;

import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;

import java.time.LocalDateTime;

public record UserProfileResponseDto(
        Long id,
        String email,
        User.Role role,
        String displayName,
        String bio,
        Long avatarMediaId,
        UserStatus status,
        Boolean emailVerified,
        LocalDateTime emailVerifiedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
