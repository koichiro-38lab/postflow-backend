package com.example.backend.dto.post;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 公開API用の投稿レスポンスDTO（一覧表示用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostPublicResponseDto {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private OffsetDateTime publishedAt;
    private PostCoverMediaSummaryDto coverMedia;
    private CategorySummaryDto category;
    private List<TagSummaryDto> tags;
}
