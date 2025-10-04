package com.example.backend.dto.user;

import com.example.backend.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 管理者によるユーザー更新リクエストDTO
 */
public record UserUpdateRequestDto(
        @Email(message = "error.email.invalid") String email,
        @Size(min = 8, message = "error.password.too_short") String password,
        @Size(max = 100, message = "error.displayName.too_long") String displayName,
        @Size(max = 5000, message = "error.bio.too_long") String bio,
        Long avatarMediaId,
        String role,
        UserStatus status) {
}
