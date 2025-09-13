package com.example.backend.dto.user;

import com.example.backend.entity.User;

public class UserMapper {

    // DTO → Entity
    public static User toEntity(UserRequestDto dto) {
        User user = new User();
        user.setEmail(dto.email());
        user.setPasswordHash(dto.password());
        user.setRole(User.Role.valueOf(dto.role().name()));
        return user;
    }

    // Entity → DTO
    public static UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getRole());
    }
}
