package com.example.backend.service;

import com.example.backend.dto.tag.TagMapper;
import com.example.backend.dto.tag.TagPublicResponseDto;
import com.example.backend.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公開API用のタグサービス
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicTagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final Clock clock;

    /**
     * 公開投稿に紐づくタグ一覧を取得
     * 
     * @return 公開タグのリスト
     */
    public List<TagPublicResponseDto> getPublicTags() {
        LocalDateTime now = LocalDateTime.now(clock);
        return tagRepository.findPublicTags(now).stream()
                .map(tagMapper::toPublicResponseDto)
                .toList();
    }
}
