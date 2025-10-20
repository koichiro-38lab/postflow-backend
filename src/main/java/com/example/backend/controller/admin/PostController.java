package com.example.backend.controller.admin;

import com.example.backend.dto.post.PostRequestDto;
import com.example.backend.dto.post.PostResponseDto;
import com.example.backend.service.PostService;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 管理画面用投稿APIコントローラー。
 * <p>
 * 投稿の検索・詳細取得・作成・更新・削除を提供。全エンドポイントで認証・RBAC制御を行う。
 * <ul>
 * <li>一覧・詳細: ADMIN/EDITORは全件、AUTHORは自分の投稿のみ</li>
 * <li>作成: ロールに応じた権限制御</li>
 * <li>更新・削除: ロール・所有権チェック</li>
 * </ul>
 * 
 * @see com.example.backend.service.PostService
 * @see com.example.backend.security.PostPolicy
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserService userService;

    /**
     * 投稿一覧を取得。
     * <p>
     * ADMIN/EDITORは全件、AUTHORは自分の投稿のみ取得可能。タイトル・スラッグ・ステータス・著者・カテゴリ・タグでフィルタ、ページング対応。
     * </p>
     * 
     * @param title      投稿タイトル（任意）
     * @param slug       スラッグ（任意）
     * @param status     ステータス（任意）
     * @param authorId   著者ID（任意）
     * @param categoryId カテゴリID（任意）
     * @param tagParam   タグ（カンマ区切り, 任意）
     * @param pageable   ページング情報
     * @param jwt        JWT認証情報
     * @return 投稿のページ
     * @throws com.example.backend.exception.AccessDeniedException 認証・権限不足
     */
    @GetMapping
    public Page<PostResponseDto> getPosts(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, name = "tag") String tagParam,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return postService.searchWithAccessControl(
                title, slug, status, authorId, categoryId, tagParam, pageable, currentUser);
    }

    /**
     * ID指定で投稿詳細を取得。
     * <p>
     * ADMIN/EDITORは全件、AUTHORは自分の投稿のみ取得可能。存在しない場合は404、権限不足は403。
     * </p>
     * 
     * @param id  投稿ID
     * @param jwt JWT認証情報
     * @return 投稿詳細
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return postService.findByIdWithAccessControl(id, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 投稿を新規作成。
     * <p>
     * ADMIN/EDITORは任意の著者で作成可能、AUTHORは自分の投稿のみ作成可能。
     * </p>
     * 
     * @param dto 投稿情報
     * @param jwt JWT認証情報
     * @return 作成された投稿詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping
    public ResponseEntity<PostResponseDto> create(
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        PostResponseDto created = postService.create(dto, currentUser);
        return ResponseEntity.created(URI.create("/api/posts/" + created.getId())).body(created);
    }

    /**
     * 投稿を更新。
     * <p>
     * ADMIN/EDITORは全件、AUTHORは自分の投稿のみ更新可能。存在しない場合は404。
     * </p>
     * 
     * @param id  投稿ID
     * @param dto 投稿情報
     * @param jwt JWT認証情報
     * @return 更新された投稿詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return postService.update(id, dto, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 投稿を削除。
     * <p>
     * ADMIN/EDITORは全件、AUTHORは自分の投稿のみ削除可能。存在しない場合は404。
     * </p>
     * 
     * @param id  投稿ID
     * @param jwt JWT認証情報
     * @return 204 No Content
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        postService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
