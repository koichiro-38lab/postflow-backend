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
 * 公開タグサービス。
 * <p>
 * 認証不要の公開API向けに、公開投稿に紐づくタグ一覧取得を提供。
 * <ul>
 * <li>公開投稿に紐づくタグのみ返却</li>
 * </ul>
 * 
 * @see com.example.backend.repository.TagRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicTagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final Clock clock;

    /**
     * 公開投稿に紐づくタグ一覧を取得。
     * <p>
     * 認証不要。現在時刻で公開状態の投稿に紐づくタグのみ返却。
     * </p>
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
