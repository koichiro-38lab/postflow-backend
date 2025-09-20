package com.example.backend.dto.media;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDownloadResponseDto {
    private String downloadUrl;
    private Instant expiresAt;
}
