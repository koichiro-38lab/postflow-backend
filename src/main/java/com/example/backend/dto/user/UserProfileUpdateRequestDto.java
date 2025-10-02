package com.example.backend.dto.user;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequestDto(
        @Size(max = 100, message = "表示名は100文字以内で入力してください") String displayName,

        @Size(max = 5000, message = "自己紹介は5000文字以内で入力してください") String bio,

        Long avatarMediaId) {
}
