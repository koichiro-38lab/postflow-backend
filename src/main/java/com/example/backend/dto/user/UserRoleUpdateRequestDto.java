package com.example.backend.dto.user;

import com.example.backend.entity.User;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequestDto(
        @NotNull(message = "error.role.required") User.Role role) {
}
