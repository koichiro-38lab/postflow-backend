package com.example.backend.dto.post;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {
    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 255)
    private String slug;

    @NotNull
    private String status;

    @Size(max = 1000)
    private String excerpt;

    @NotBlank
    private String contentJson;

    private Long coverMediaId;
    private Long authorId; // Optional: 新規作成時のみ必須、編集時は既存authorを保持
    private Long categoryId;
    private List<String> tags;
    private List<Long> tagIds;
    private OffsetDateTime publishedAt;
}
