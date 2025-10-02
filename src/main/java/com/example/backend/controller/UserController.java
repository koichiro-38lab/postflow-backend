package com.example.backend.controller;

import com.example.backend.dto.user.UserProfileResponseDto;
import com.example.backend.dto.user.UserProfileUpdateRequestDto;
import com.example.backend.security.UserPolicy;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPolicy userPolicy;

    /**
     * 自分のプロフィール取得
     * GET /api/users/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        var currentUser = userService.getCurrentUser(jwt);
        var profile = userService.getMyProfile(currentUser);
        return ResponseEntity.ok(profile);
    }

    /**
     * 自分のプロフィール更新
     * PUT /api/users/me
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
