package com.example.backend.controller.admin;

import com.example.backend.dto.user.UserMapper;
import com.example.backend.dto.user.UserProfileResponseDto;
import com.example.backend.dto.user.UserProfileUpdateRequestDto;
import com.example.backend.dto.user.UserRequestDto;
import com.example.backend.dto.user.UserResponseDto;
import com.example.backend.dto.user.UserRoleUpdateRequestDto;
import com.example.backend.dto.user.UserStatusUpdateRequestDto;
import com.example.backend.dto.user.UserUpdateRequestDto;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.security.UserPolicy;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * 管理画面用ユーザーAPIコントローラー。
 * <p>
 * ユーザーの作成・一覧・詳細・更新・削除・ロール/ステータス変更・自身プロフィール取得/更新を提供。全エンドポイントで認証・RBAC制御を行う。
 * <ul>
 * <li>管理者のみ: 作成・一覧・詳細・更新・削除・ロール/ステータス変更</li>
 * <li>全認証ユーザー: 自身のプロフィール取得・更新</li>
 * </ul>
 * 
 * @see com.example.backend.service.UserService
 * @see com.example.backend.security.UserPolicy
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPolicy userPolicy;

    /**
     * ユーザーを新規作成。
     * <p>
     * 管理者のみ利用可能。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @param dto ユーザー作成情報
     * @return 作成されたユーザー詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserRequestDto dto) {
        var currentUser = userService.getCurrentUser(jwt);
        UserResponseDto user = userService.createUser(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * 全ユーザーをページング取得。
     * <p>
     * 管理者のみ利用可能。ステータス・ロールでフィルタ可能。
     * </p>
     * 
     * @param jwt      JWT認証情報
     * @param pageable ページング情報
     * @param status   ユーザーステータス（任意）
     * @param role     ユーザーロール（任意）
     * @return ユーザー一覧ページ
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) User.Role role) { // ロールパラメータを追加
        var currentUser = userService.getCurrentUser(jwt);
        Page<UserResponseDto> users = userService.findAllWithPagination(pageable, status, role, currentUser);
        return ResponseEntity.ok(users);
    }

    /**
     * ユーザー詳細を取得。
     * <p>
     * 管理者のみ利用可能。存在しない場合は404。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @param id  ユーザーID
     * @return ユーザー詳細
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> getUserById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        var currentUser = userService.getCurrentUser(jwt);
        return ResponseEntity.ok(userService.getUserById(id, currentUser));
    }

    /**
     * ユーザー情報を更新。
     * <p>
     * 管理者のみ利用可能。存在しない場合は404。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @param id  ユーザーID
     * @param dto ユーザー更新情報
     * @return 更新されたユーザー詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDto dto) {
        var currentUser = userService.getCurrentUser(jwt);
        UserResponseDto user = userService.updateUserByAdmin(id, dto, currentUser);
        return ResponseEntity.ok(user);
    }

    /**
     * ユーザーステータスを変更。
     * <p>
     * 管理者のみ利用可能。存在しない場合は404。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @param id  ユーザーID
     * @param dto ユーザーステータス更新情報
     * @return 更新されたユーザー詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequestDto dto) {
        var currentUser = userService.getCurrentUser(jwt);
        var updated = userService.updateUserStatus(id, dto.status(), currentUser);
        return ResponseEntity.ok(updated);
    }

    /**
     * ユーザーロールを変更。
     * <p>
     * 管理者のみ利用可能。存在しない場合は404。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @param id  ユーザーID
     * @param dto ユーザーロール更新情報
     * @return 更新されたユーザー詳細
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequestDto dto) {
        var currentUser = userService.getCurrentUser(jwt);
        var updated = userService.updateUserRole(id, dto.role(), currentUser);
        return ResponseEntity.ok(updated);
    }

    /**
     * ユーザーを削除。
     * <p>
     * 管理者のみ利用可能。存在しない場合は404。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @param id  ユーザーID
     * @return 204 No Content
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        var currentUser = userService.getCurrentUser(jwt);
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * 自分のユーザー情報を取得。
     * <p>
     * 全認証ユーザーが利用可能。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @return 自分のユーザー情報
     * @throws com.example.backend.exception.AccessDeniedException 認証失敗時
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponseDto getMe(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        var user = userService.getCurrentUser(jwt);
        return UserMapper.toResponseDto(user);
    }

    /**
     * 自分のプロフィールを更新。
     * <p>
     * 全認証ユーザーが利用可能。自分自身のみ更新可能。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @param dto プロフィール更新情報
     * @return 更新されたプロフィール情報
     * @throws com.example.backend.exception.AccessDeniedException          権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponseDto> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileUpdateRequestDto dto) {
        var currentUser = userService.getCurrentUser(jwt);
        // 権限チェック（自分のプロフィールのみ更新可能）
        userPolicy.checkUpdateProfile(currentUser, currentUser);
        var updated = userService.updateMyProfile(currentUser, dto);
        return ResponseEntity.ok(updated);
    }
}
