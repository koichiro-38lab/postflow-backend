package com.example.backend.controller.pub;

import com.example.backend.dto.category.CategoryPublicResponseDto;
import com.example.backend.service.PublicCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公開カテゴリAPIコントローラー。
 * <p>
 * 認証不要で公開投稿に紐づくカテゴリ一覧を取得可能。
 * </p>
 * 
 * @see com.example.backend.service.PublicCategoryService
 */
@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final PublicCategoryService publicCategoryService;

    /**
     * 公開投稿に紐づくカテゴリ一覧を取得。
     * <p>
     * 認証不要。公開状態の投稿に紐づくカテゴリのみ返却。
     * </p>
     * 
     * @return 公開カテゴリのリスト
     */
    @GetMapping
    public ResponseEntity<List<CategoryPublicResponseDto>> getCategories() {
        return ResponseEntity.ok(publicCategoryService.getPublicCategories());
    }
}
