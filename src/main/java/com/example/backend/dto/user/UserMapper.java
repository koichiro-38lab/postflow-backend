package com.example.backend.dto.user;

import com.example.backend.entity.User;

public class UserMapper {

    // DTO → Entity
    public static User toEntity(UserRequestDto dto) {
        User user = new User();
        user.setEmail(dto.email());
        user.setRole(User.Role.valueOf(dto.role()));
        return user;
    }

    // Entity → DTO（管理画面用、全フィールド）
    public static UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarMedia() != null ? user.getAvatarMedia().getId() : null,
                user.getStatus(),
                user.getEmailVerified(),
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    // Entity → ProfileDTO（自分のプロフィール用）
    public static UserProfileResponseDto toProfileResponseDto(User user) {
        return new UserProfileResponseDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarMedia() != null ? user.getAvatarMedia().getId() : null,
                user.getStatus(),
                user.getEmailVerified(),
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    // ProfileUpdateDTO → Entity（プロフィール更新の適用）
    public static void applyProfileUpdate(User user, UserProfileUpdateRequestDto dto) {
        user.setDisplayName(dto.displayName());
        user.setBio(dto.bio());
        // avatarMediaId は Service 層で Media エンティティを取得して設定
    }
}
