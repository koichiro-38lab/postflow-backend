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
 * 公開API: カテゴリエンドポイント（認証不要）
 */
@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final PublicCategoryService publicCategoryService;

    /**
     * 公開投稿に紐づくカテゴリ一覧を取得
     * GET /api/public/categories
     * 
     * @return 公開カテゴリのリスト
     */
    @GetMapping
    public ResponseEntity<List<CategoryPublicResponseDto>> getCategories() {
        return ResponseEntity.ok(publicCategoryService.getPublicCategories());
    }
}
