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

/**
 * 管理画面用タグAPIコントローラー。
 * <p>
 * タグの一覧取得・作成・更新・削除を提供。全エンドポイントで認証・RBAC制御を行う。
 * <ul>
 * <li>一覧・詳細: 認証ユーザー全員</li>
 * <li>作成: ADMIN/EDITORのみ</li>
 * <li>更新・削除: ADMIN/EDITORのみ、参照整合性あり</li>
 * </ul>
 * 
 * @see com.example.backend.service.TagService
 */
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 全タグを取得。
     * <p>
     * 認証ユーザー全員が利用可能。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @return タグ一覧
     * @throws com.example.backend.exception.AccessDeniedException 認証失敗時
     */
    @GetMapping
    public List<TagResponseDto> list(@AuthenticationPrincipal Jwt jwt) {
        requireUser(jwt); // 認証のみ必須
        return tagService.findAll();
    }

    /**
     * タグを新規作成。
     * <p>
     * ADMIN/EDITORのみ作成可能。
     * </p>
     * 
     * @param jwt     JWT認証情報
     * @param request タグ作成リクエスト
     * @return 作成されたタグ詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping
    public ResponseEntity<TagResponseDto> create(@AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TagRequestDto request) {
        User currentUser = requireUser(jwt);
        TagResponseDto created = tagService.create(request, currentUser.getRole());
        return ResponseEntity.created(URI.create("/api/admin/tags/" + created.getId())).body(created);
    }

    /**
     * タグを更新。
     * <p>
     * ADMIN/EDITORのみ更新可能。存在しない場合は404。
     * </p>
     * 
     * @param id      タグID
     * @param jwt     JWT認証情報
     * @param request 更新情報
     * @return 更新されたタグ詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PutMapping("/{id}")
    public TagResponseDto update(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TagRequestDto request) {
        User currentUser = requireUser(jwt);
        return tagService.update(id, request, currentUser.getRole());
    }

    /**
     * タグを削除。
     * <p>
     * ADMIN/EDITORのみ削除可能。投稿で参照されている場合は409（TagInUseException）、存在しない場合は404。
     * </p>
     * 
     * @param id  タグID
     * @param jwt JWT認証情報
     * @return 204 No Content
     * @throws com.example.backend.exception.TagInUseException     参照整合性違反時
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        if (postRepository.existsByTags_Id(id)) {
            throw new TagInUseException(id);
        }
        tagService.delete(id, currentUser.getRole());
        return ResponseEntity.noContent().build();
    }

    /**
     * JWTから認証ユーザーを取得。
     * <p>
     * 認証情報が無い場合やユーザーが見つからない場合はAccessDeniedException。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @return 認証済みユーザー
     * @throws com.example.backend.exception.AccessDeniedException 認証失敗時
     */
    private User requireUser(Jwt jwt) {
        if (jwt == null) {
            throw new com.example.backend.exception.AccessDeniedException("Authentication required");
        }
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.example.backend.exception.AccessDeniedException("Authentication required"));
    }
}
