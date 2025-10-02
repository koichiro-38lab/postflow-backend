package com.example.backend.dto.user;

import com.example.backend.entity.User;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequestDto(
        @NotNull(message = "ロールは必須です") User.Role role) {
}
