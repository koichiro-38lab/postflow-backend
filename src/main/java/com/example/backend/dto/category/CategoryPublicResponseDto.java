package com.example.backend.dto.category;

import lombok.*;

/**
 * 公開API用のカテゴリレスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryPublicResponseDto {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Integer sortOrder;
}
