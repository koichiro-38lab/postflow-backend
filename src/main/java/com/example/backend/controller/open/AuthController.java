package com.example.backend.controller.open;

import com.example.backend.dto.auth.AuthResponseDto;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.auth.RefreshRequestDto;
import com.example.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        String ip = resolveIp(http);
        return ResponseEntity.ok(authService.login(request, ua, ip));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshRequestDto request,
            HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        String ip = resolveIp(http);
        return ResponseEntity.ok(authService.refresh(request.refreshToken(), ua, ip));
    }

    private String resolveIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // 先頭のクライアントIPを採用
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return http.getRemoteAddr();
    }
}
