package com.example.backend.dto.tag;

import lombok.*;

/**
 * 公開API用のタグレスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagPublicResponseDto {
    private Long id;
    private String name;
    private String slug;
}
