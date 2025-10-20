package com.example.backend.controller.pub;

import com.example.backend.dto.post.PostPublicDetailResponseDto;
import com.example.backend.dto.post.PostPublicResponseDto;
import com.example.backend.service.PublicPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 公開投稿APIコントローラー。
 * <p>
 * 認証不要で公開投稿の一覧・詳細取得が可能。
 * </p>
 * 
 * @see com.example.backend.service.PublicPostService
 */
@RestController
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
public class PublicPostController {

    private final PublicPostService publicPostService;

    /**
     * 公開投稿一覧を取得。
     * <p>
     * 認証不要。タグ・カテゴリでフィルタ可能。ページング対応。
     * </p>
     * 
     * @param pageable   ページング情報（デフォルト: publishedAt DESC）
     * @param tag        タグスラッグ（任意）
     * @param category   カテゴリスラッグ（任意）
     * @param categories カテゴリスラッグ複数（任意, カンマ区切り）
     * @return 公開投稿のページ
     */
    @GetMapping
    public ResponseEntity<Page<PostPublicResponseDto>> getPosts(
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categories) {
        return ResponseEntity.ok(publicPostService.getPublicPosts(pageable, tag, category, categories));
    }

    /**
     * スラッグ指定で公開投稿詳細を取得。
     * <p>
     * 認証不要。存在しない場合は404。
     * </p>
     * 
     * @param slug 投稿スラッグ
     * @return 投稿詳細（SEO/OGP情報含む）
     */
    @GetMapping("/{slug}")
    public ResponseEntity<PostPublicDetailResponseDto> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(publicPostService.getPublicPostBySlug(slug));
    }
}
