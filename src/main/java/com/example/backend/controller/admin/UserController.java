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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPolicy userPolicy;

    /**
     * ユーザー作成
     * POST /api/admin/users
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
     * ユーザー一覧取得（ページング、ステータスフィルタ対応）
     * GET /api/admin/users?page=0&size=20&status=ACTIVE&role=ADMIN
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
     * ユーザー詳細取得
     * GET /api/admin/users/{id}
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
     * ユーザー情報更新（管理者用）
     * PUT /api/admin/users/{id}
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
     * ユーザーステータス変更
     * PATCH /api/admin/users/{id}/status
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
     * ユーザーロール変更
     * PATCH /api/admin/users/{id}/role
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
     * ユーザー削除
     * DELETE /api/admin/users/{id}
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
     * 自分のユーザー情報取得
     * GET /api/admin/users/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponseDto getMe(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        var user = userService.getCurrentUser(jwt);
        return UserMapper.toResponseDto(user);
    }

    /**
     * 自分のプロフィール更新
     * PUT /api/admin/users/me
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
