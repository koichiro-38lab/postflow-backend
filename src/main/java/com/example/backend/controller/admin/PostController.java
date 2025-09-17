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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<PostResponseDto> getPosts(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsStringList("roles").get(0); // 例: ["EDITOR"]
        Long userId = jwt.getClaim("sub") != null ? getUserIdFromJwt(jwt) : null;

        if (!"ADMIN".equals(role) && !"EDITOR".equals(role)) {
            // ADMINもしくはEDITOR以外はauthorIdを自分のIDに強制
            authorId = userId;
        }

        // パラメータがすべてnullの場合は全件取得、それ以外は検索を実行
        if (title == null && slug == null && status == null && authorId == null && categoryId == null)

        {
            return postService.findAll(pageable);
        } else {
            return postService.search(title, slug, status, authorId, categoryId, pageable);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsStringList("roles").get(0);
        Long userId = getUserIdFromJwt(jwt);

        return postService.findById(id, userId, User.Role.valueOf(role))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PostResponseDto> create(
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsStringList("roles").get(0);
        Long userId = getUserIdFromJwt(jwt);

        PostResponseDto created = postService.create(dto, userId, User.Role.valueOf(role));
        return ResponseEntity.created(URI.create("/api/posts/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsStringList("roles").get(0);
        Long userId = getUserIdFromJwt(jwt);

        return postService.update(id, dto, userId, User.Role.valueOf(role))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsStringList("roles").get(0);
        Long userId = getUserIdFromJwt(jwt);

        postService.delete(id, userId, User.Role.valueOf(role));
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromJwt(Jwt jwt) {
        String email = jwt.getSubject();
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}
