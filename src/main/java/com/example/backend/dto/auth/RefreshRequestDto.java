package com.example.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(
        @NotBlank(message = "error.refresh_token.required") String refreshToken) {}
