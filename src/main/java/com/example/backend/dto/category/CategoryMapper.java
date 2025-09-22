package com.example.backend.dto.category;

import com.example.backend.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    public CategoryResponseDto toResponseDto(Category category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() != null ? new CategorySummaryDto(
                        category.getParent().getId(),
                        category.getParent().getName(),
                        category.getParent().getSlug()) : null,
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public void applyToEntity(Category category, CategoryRequestDto dto) {
        category.setName(dto.name());
        category.setSlug(dto.slug());
        // parentはServiceで設定
    }
}