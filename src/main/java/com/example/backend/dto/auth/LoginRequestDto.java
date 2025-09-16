package com.example.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "error.email.required") @Email(message = "error.email.invalid") String email,
        @NotBlank(message = "error.password.required") String password) {}
