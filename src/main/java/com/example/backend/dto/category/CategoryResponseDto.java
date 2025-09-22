package com.example.backend.dto.category;

import java.time.LocalDateTime;

public record CategoryResponseDto(
        Long id,
        String name,
        String slug,
        CategorySummaryDto parent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}