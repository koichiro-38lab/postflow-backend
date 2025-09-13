package com.example.backend.dto.common;

import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        List<ValidationError> messages) {
    public record ValidationError(String field, String code) {
    }
}