package com.example.backend.dto.media;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponseDto {
    private Long id;
    private String filename;
    private String storageKey;
    private String mime;
    private Long bytes;
    private Integer width;
    private Integer height;
    private String altText;
    private String publicUrl;
    private LocalDateTime createdAt;
    private CreatedBySummary createdBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedBySummary {
        private Long id;
        private String displayName;
        private String role;
    }
}
