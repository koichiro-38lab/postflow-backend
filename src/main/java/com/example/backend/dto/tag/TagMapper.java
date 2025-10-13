package com.example.backend.dto.tag;

import com.example.backend.entity.Tag;
import com.example.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagMapper {
    private final PostRepository postRepository;

    public TagResponseDto toResponseDto(Tag tag) {
        // タグに関連する投稿数を取得
        long postCount = postRepository.countByTags_Id(tag.getId());

        return TagResponseDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .postCount(postCount)
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }

    // 公開API用: シンプルなDTO変換
    public TagPublicResponseDto toPublicResponseDto(Tag tag) {
        return TagPublicResponseDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }
}
