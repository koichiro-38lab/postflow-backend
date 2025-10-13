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
 * 公開API: 投稿エンドポイント（認証不要）
 */
@RestController
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
public class PublicPostController {

    private final PublicPostService publicPostService;

    /**
     * 公開投稿一覧を取得
     * GET /api/public/posts?page=0&size=10&tag=spring&category=tech
     * 
     * @param pageable ページング情報（デフォルト: publishedAt DESC）
     * @param tag      タグスラッグ（オプション）
     * @param category カテゴリスラッグ（オプション）
     * @return 公開投稿のページ
     */
    @GetMapping
    public ResponseEntity<Page<PostPublicResponseDto>> getPosts(
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(publicPostService.getPublicPosts(pageable, tag, category));
    }

    /**
     * スラッグによる公開投稿詳細を取得
     * GET /api/public/posts/{slug}
     * 
     * @param slug 投稿スラッグ
     * @return 投稿詳細（SEO/OGP情報含む）
     */
    @GetMapping("/{slug}")
    public ResponseEntity<PostPublicDetailResponseDto> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(publicPostService.getPublicPostBySlug(slug));
    }
}
