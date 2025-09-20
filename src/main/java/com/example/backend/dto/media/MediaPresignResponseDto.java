package com.example.backend.dto.media;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaPresignResponseDto {
    private String uploadUrl;
    private String storageKey;
    private Instant expiresAt;
    private Map<String, String> headers;
}
