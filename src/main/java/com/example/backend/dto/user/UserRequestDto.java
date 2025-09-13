package com.example.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.example.backend.entity.User;

public record UserRequestDto(
    @NotBlank(message = "error.email.required")
    @Email(message = "error.email.invalid")
    String email,

    @NotBlank(message = "error.password.required")
    @Size(min = 8, message = "error.password.too_short")
    String password,

    User.Role role
) {}
