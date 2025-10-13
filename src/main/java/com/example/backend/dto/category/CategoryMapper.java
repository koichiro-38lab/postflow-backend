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
                category.getSortOrder(),
                0, // postCount は別途設定
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public CategoryResponseDto toResponseDtoWithPostCount(Category category, int postCount) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() != null ? new CategorySummaryDto(
                        category.getParent().getId(),
                        category.getParent().getName(),
                        category.getParent().getSlug()) : null,
                category.getSortOrder(),
                postCount,
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public void applyToEntity(Category category, CategoryRequestDto dto) {
        category.setName(dto.name());
        category.setSlug(dto.slug());
        // parentはServiceで設定
    }

    // 公開API用: シンプルなDTO変換
    public CategoryPublicResponseDto toPublicResponseDto(Category category) {
        return CategoryPublicResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }
}