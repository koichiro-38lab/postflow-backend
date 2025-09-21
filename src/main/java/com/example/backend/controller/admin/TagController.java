package com.example.backend.controller.admin;

import com.example.backend.dto.tag.TagRequestDto;
import com.example.backend.dto.tag.TagResponseDto;
import com.example.backend.entity.User;
import com.example.backend.exception.TagInUseException;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.TagService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // タグ一覧取得 (認証ユーザーのみ)
    @GetMapping
    public List<TagResponseDto> list(@AuthenticationPrincipal Jwt jwt) {
        requireUser(jwt); // 認証のみ必須
        return tagService.findAll();
    }

    // タグ作成 (ADMIN, EDITOR)
    @PostMapping
    public ResponseEntity<TagResponseDto> create(@AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TagRequestDto request) {
        User currentUser = requireUser(jwt);
        TagResponseDto created = tagService.create(request, currentUser.getRole());
        return ResponseEntity.created(URI.create("/api/admin/tags/" + created.getId())).body(created);
    }

    // タグ更新 (ADMIN, EDITOR)
    @PutMapping("/{id}")
    public TagResponseDto update(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TagRequestDto request) {
        User currentUser = requireUser(jwt);
        return tagService.update(id, request, currentUser.getRole());
    }

    // タグ削除 (ADMIN, EDITOR)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        if (postRepository.existsByTags_Id(id)) {
            throw new TagInUseException(id);
        }
        tagService.delete(id, currentUser.getRole());
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
}
