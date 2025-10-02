package com.example.backend.dto.user;

import com.example.backend.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequestDto(
        @NotNull(message = "ステータスは必須です") UserStatus status) {
}
