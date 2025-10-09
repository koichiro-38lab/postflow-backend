package com.example.backend.dto.media;

import com.example.backend.entity.Media;
import org.springframework.stereotype.Component;

@Component
public class MediaMapper {
    public MediaResponseDto toResponseDto(Media media, String publicUrl) {
        return MediaResponseDto.builder()
                .id(media.getId())
                .filename(media.getFilename())
                .storageKey(media.getStorageKey())
                .mime(media.getMime())
                .bytes(media.getBytes())
                .width(media.getWidth())
                .height(media.getHeight())
                .altText(media.getAltText())
                .publicUrl(publicUrl)
                .createdAt(media.getCreatedAt())
                .createdBy(MediaResponseDto.CreatedBySummary.builder()
                        .id(media.getCreatedBy().getId())
                        .displayName(media.getCreatedBy().getDisplayName())
                        .role(media.getCreatedBy().getRole().name())
                        .build())
                .build();
    }
}
