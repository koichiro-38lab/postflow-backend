package com.example.backend.service;

import com.example.backend.dto.category.CategoryMapper;
import com.example.backend.dto.category.CategoryPublicResponseDto;
import com.example.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公開API用のカテゴリサービス
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final Clock clock;

    /**
     * 公開投稿に紐づくカテゴリ一覧を取得
     * 
     * @return 公開カテゴリのリスト
     */
    public List<CategoryPublicResponseDto> getPublicCategories() {
        LocalDateTime now = LocalDateTime.now(clock);
        return categoryRepository.findPublicCategories(now).stream()
                .map(categoryMapper::toPublicResponseDto)
                .toList();
    }
}
