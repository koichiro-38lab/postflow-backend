package com.example.backend.controller.admin;

import com.example.backend.dto.post.PostRequestDto;
import com.example.backend.dto.post.PostResponseDto;
import com.example.backend.service.PostService;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;

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
        User currentUser = requireUser(jwt);
        String role = currentUser.getRole().name();
        Long userId = currentUser.getId();

        if (!"ADMIN".equals(role) && !"EDITOR".equals(role)) {
            authorId = userId;
        }
        List<String> tags = parseTags(tagParam);
        if (title == null && slug == null && status == null && authorId == null && categoryId == null
                && tags.isEmpty()) {
            return postService.findAll(pageable);
        } else {
            return postService.search(title, slug, status, authorId, categoryId, tags, pageable);
        }
    }

    // 投稿IDで取得 (認証ユーザーのみ、ADMIN, EDITORは全件、AUTHORは自分の投稿のみ)
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        return postService.findById(id, currentUser.getId(), currentUser.getRole())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 投稿作成 (ADMIN, EDITOR, AUTHOR)
    @PostMapping
    public ResponseEntity<PostResponseDto> create(
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        PostResponseDto created = postService.create(dto, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.created(URI.create("/api/posts/" + created.getId())).body(created);
    }

    // 投稿更新 (ADMIN, EDITORは全件、AUTHORは自分の投稿のみ)
    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        return postService.update(id, dto, currentUser.getId(), currentUser.getRole())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 投稿削除 (ADMIN, EDITORは全件、AUTHORは自分の投稿のみ)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        postService.delete(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }

    // ユーザー取得
    private User requireUser(Jwt jwt) {
        if (jwt == null) {
            throw new com.example.backend.exception.AccessDeniedException("Authentication required");
        }
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.example.backend.exception.AccessDeniedException("Authentication required"));
    }

    // カンマ区切りのタグ文字列をリストに変換
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
