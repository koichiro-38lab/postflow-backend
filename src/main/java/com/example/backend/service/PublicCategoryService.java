package com.example.backend.service;

import com.example.backend.dto.category.CategoryMapper;
import com.example.backend.dto.category.CategoryPublicResponseDto;
import com.example.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 公開カテゴリサービス。
 * <p>
 * 認証不要の公開API向けに、公開投稿に紐づくカテゴリ一覧取得を提供。
 * <ul>
 * <li>公開投稿に紐づくカテゴリのみ返却</li>
 * <li>sort_order昇順で返却</li>
 * </ul>
 * 
 * @see com.example.backend.repository.CategoryRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final Clock clock;

    /**
     * 公開投稿に紐づくカテゴリ一覧を取得。
     * <p>
     * 認証不要。現在時刻で公開状態の投稿に紐づくカテゴリのみ返却。sort_order昇順。
     * </p>
     * 
     * @return 公開カテゴリのリスト（sort_order昇順）
     */
    public List<CategoryPublicResponseDto> getPublicCategories() {
        LocalDateTime now = LocalDateTime.now(clock);
        return categoryRepository.findPublicCategories(now).stream()
                .map(categoryMapper::toPublicResponseDto)
                .sorted(Comparator.comparing(CategoryPublicResponseDto::getSortOrder))
                .toList();
    }
}
