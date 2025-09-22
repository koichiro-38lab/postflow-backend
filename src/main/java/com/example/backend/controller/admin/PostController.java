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

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserService userService;

    // 投稿一覧取得 (認証ユーザーのみ、ADMIN, EDITORは全件、AUTHORは自分の投稿のみ)
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

    // 投稿IDで取得 (認証ユーザーのみ、ADMIN, EDITORは全件、AUTHORは自分の投稿のみ)
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return postService.findByIdWithAccessControl(id, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 投稿作成 (ADMIN, EDITOR, AUTHOR)
    @PostMapping
    public ResponseEntity<PostResponseDto> create(
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        PostResponseDto created = postService.create(dto, currentUser);
        return ResponseEntity.created(URI.create("/api/posts/" + created.getId())).body(created);
    }

    // 投稿更新 (ADMIN, EDITORは全件、AUTHORは自分の投稿のみ)
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

    // 投稿削除 (ADMIN, EDITORは全件、AUTHORは自分の投稿のみ)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        postService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // コントローラーは認証・リクエスト受信・サービス呼び出し・レスポンス返却のみ担当
}
