package com.example.backend.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDto(
        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must be less than 255 characters") String name,

        @NotBlank(message = "Slug is required") @Size(max = 255, message = "Slug must be less than 255 characters") String slug,

        Long parentId) {
}