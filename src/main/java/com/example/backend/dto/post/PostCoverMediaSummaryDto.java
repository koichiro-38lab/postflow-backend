package com.example.backend.dto.post;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCoverMediaSummaryDto {
    private String url;
    private Integer width;
    private Integer height;
    private String altText;
}
