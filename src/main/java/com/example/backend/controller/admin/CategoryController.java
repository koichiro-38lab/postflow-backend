package com.example.backend.controller.admin;

import com.example.backend.dto.category.CategoryRequestDto;
import com.example.backend.dto.category.CategoryResponseDto;
import com.example.backend.dto.category.CategoryReorderRequestDto;
import com.example.backend.service.CategoryService;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.List;

/**
 * 管理画面用カテゴリAPIコントローラー。
 * <p>
 * カテゴリの一覧取得、詳細取得、作成、更新、削除、並び順変更を提供。
 * 全エンドポイントで認証・RBAC制御を行う。
 * </p>
 * <ul>
 * <li>一覧取得: 親子関係・sort_order順、投稿数付き</li>
 * <li>詳細取得: ID指定、アクセス制御あり</li>
 * <li>作成/更新/削除: RBAC・バリデーション・例外ハンドリング</li>
 * <li>並び順変更: 複数カテゴリのsort_order一括更新</li>
 * </ul>
 * 
 * @see com.example.backend.service.CategoryService
 * @see com.example.backend.security.CategoryPolicy
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final UserService userService;

    /**
     * 全カテゴリを親子関係・sort_order順で取得（投稿数付き）。
     * <p>
     * 管理者・編集者のみ利用可能。認可はCategoryPolicyで判定。
     * </p>
     * 
     * @param jwt JWT認証情報（認可判定用）
     * @return カテゴリ一覧（投稿数付き）
     */
    @GetMapping
    public List<CategoryResponseDto> getCategories(@AuthenticationPrincipal Jwt jwt) {
        return categoryService.findAllWithPostCount();
    }

    /**
     * IDでカテゴリ詳細を取得（アクセス制御付き）。
     * <p>
     * 存在しない場合は404、権限不足は403。
     * </p>
     * 
     * @param id  カテゴリID
     * @param jwt JWT認証情報
     * @return カテゴリ詳細（存在しない場合は404）
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return categoryService.findByIdWithAccessControl(id, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * カテゴリを新規作成。
     * <p>
     * 親ID指定時は存在チェックあり。バリデーションエラー時は400。
     * </p>
     * 
     * @param dto カテゴリ作成リクエスト
     * @param jwt JWT認証情報
     * @return 作成されたカテゴリ
     * @throws com.example.backend.exception.CategoryNotFoundException      親ID不正時
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping
    public ResponseEntity<CategoryResponseDto> create(
            @RequestBody @Valid CategoryRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        CategoryResponseDto created = categoryService.create(dto, currentUser);
        return ResponseEntity.created(URI.create("/api/admin/categories/" + created.id())).body(created);
    }

    /**
     * ID指定でカテゴリを更新。
     * <p>
     * 存在しない場合は404、バリデーションエラー時は400。
     * </p>
     * 
     * @param id  カテゴリID
     * @param dto 更新内容
     * @param jwt JWT認証情報
     * @return 更新されたカテゴリ（存在しない場合は404）
     * @throws com.example.backend.exception.CategoryNotFoundException      親ID不正時
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid CategoryRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return categoryService.update(id, dto, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ID指定でカテゴリを削除。
     * <p>
     * 投稿が紐付く場合は409（CategoryInUseException）、存在しない場合は404。
     * </p>
     * 
     * @param id  カテゴリID
     * @param jwt JWT認証情報
     * @return 204 No Content
     * @throws com.example.backend.exception.CategoryInUseException    投稿紐付け時
     * @throws com.example.backend.exception.CategoryNotFoundException 存在しない場合
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        categoryService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * カテゴリの並び順（sort_order）を一括更新。
     * <p>
     * 複数カテゴリのsort_orderをまとめて変更。バリデーションエラー時は400。
     * </p>
     * 
     * @param reorderRequests 並び順更新リクエスト一覧
     * @param jwt             JWT認証情報
     * @return 200 OK
     * @throws com.example.backend.exception.CategoryNotFoundException      存在しないカテゴリID指定時
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderCategories(
            @RequestBody @Valid List<CategoryReorderRequestDto> reorderRequests,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        categoryService.reorderCategories(reorderRequests, currentUser);
        return ResponseEntity.ok().build();
    }
}