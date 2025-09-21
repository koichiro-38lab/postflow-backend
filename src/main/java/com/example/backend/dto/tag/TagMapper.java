package com.example.backend.dto.tag;

import com.example.backend.entity.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {
    public TagResponseDto toResponseDto(Tag tag) {
        return TagResponseDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}
