package com.example.backend.dto.post;

import lombok.*;
import java.time.LocalDateTime;
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
    private Long coverMediaId;
    private Long authorId;
    private Long categoryId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> tagIds;
}
