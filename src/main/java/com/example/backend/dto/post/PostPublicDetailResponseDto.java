package com.example.backend.dto.post;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 公開API用の投稿詳細レスポンスDTO（SEO/OGP情報含む）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostPublicDetailResponseDto {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String contentJson;
    private OffsetDateTime publishedAt;
    private PostCoverMediaSummaryDto coverMedia;
    private CategorySummaryDto category;
    private List<TagSummaryDto> tags;
    private AuthorSummaryDto author;
    // OGPフィールド
    private String ogTitle;
    private String ogDescription;
    private String ogImage;
    private String ogUrl;
}
