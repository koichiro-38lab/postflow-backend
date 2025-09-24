package com.example.backend.dto.post;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private Long id;
    private String title;
    private String slug;
    private String status;
    private String excerpt;
    private String contentJson;
    private CoverMediaSummaryDto coverMedia;
    private AuthorSummaryDto author;
    private CategorySummaryDto category;
    private List<TagSummaryDto> tags;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
