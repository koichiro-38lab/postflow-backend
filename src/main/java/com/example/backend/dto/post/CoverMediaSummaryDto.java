package com.example.backend.dto.post;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverMediaSummaryDto {
    private Long id;
    private String filename;
    private String mime;
    private Integer width;
    private Integer height;
    private Long bytes;
    private String altText;
}
