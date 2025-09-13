package com.example.backend.dto.user;
import com.example.backend.entity.User;

public record UserResponseDto(
    Long id,
    String email,
    User.Role role
) {}