package com.example.backend.dto.category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryReorderRequestDto {
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Sort order is required")
    @Min(value = 0, message = "Sort order must be non-negative")
    private Integer newSortOrder;
}