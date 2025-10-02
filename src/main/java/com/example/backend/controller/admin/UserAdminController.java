package com.example.backend.controller.admin;

import com.example.backend.dto.user.UserMapper;
import com.example.backend.dto.user.UserRequestDto;
import com.example.backend.dto.user.UserResponseDto;
import com.example.backend.dto.user.UserRoleUpdateRequestDto;
import com.example.backend.dto.user.UserStatusUpdateRequestDto;
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
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;
    private final UserPolicy userPolicy;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
        UserResponseDto user = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * ユーザー一覧取得（ページング、ステータスフィルタ対応）
     * GET /api/admin/users?page=0&size=20&status=ACTIVE
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) UserStatus status) {
        var currentUser = userService.getCurrentUser(jwt);
        userPolicy.checkManageUsers(currentUser);
        Page<UserResponseDto> users = userService.findAllWithPagination(pageable, status);
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
        userPolicy.checkManageUsers(currentUser);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @RequestBody UserRequestDto dto) {
        UserResponseDto user = userService.updateUser(id, dto);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
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
        var targetUser = userService.getUserById(id);
        // 権限チェック（ADMIN のみ、自分以外）
        userPolicy.checkChangeUserStatus(currentUser,
                User.builder().id(targetUser.id()).build());
        var updated = userService.updateUserStatus(id, dto.status());
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
        var targetUser = userService.getUserById(id);
        // 権限チェック（ADMIN のみ、自分以外）
        userPolicy.checkChangeUserRole(currentUser,
                User.builder().id(targetUser.id()).build());
        var updated = userService.updateUserRole(id, dto.role());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponseDto getMe(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        var user = userService.getCurrentUser(jwt);
        return UserMapper.toResponseDto(user);
    }
}
