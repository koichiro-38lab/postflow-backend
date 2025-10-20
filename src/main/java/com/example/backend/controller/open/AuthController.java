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

/**
 * 公開認証APIコントローラー。
 * <p>
 * ログイン・リフレッシュトークン発行を提供。全エンドポイントでバリデーション・認証エラー時の統一レスポンス。
 * <ul>
 * <li>ログイン: JWTアクセストークン・リフレッシュトークン発行</li>
 * <li>リフレッシュ: 有効なリフレッシュトークンでアクセストークン再発行</li>
 * </ul>
 * 
 * @see com.example.backend.service.AuthService
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * ログイン。
     * <p>
     * メール・パスワードで認証し、JWTアクセストークン・リフレッシュトークンを発行。
     * </p>
     * 
     * @param request ログインリクエスト
     * @param http    HttpServletRequest（User-Agent, IP取得用）
     * @return 認証レスポンス（アクセストークン・リフレッシュトークン）
     * @throws com.example.backend.exception.AuthException                  認証失敗時
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        String ip = resolveIp(http);
        AuthResponseDto response = authService.login(request, ua, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * リフレッシュトークンでアクセストークンを再発行。
     * <p>
     * 有効なリフレッシュトークンを検証し、新たなアクセストークン・リフレッシュトークンを発行。
     * </p>
     * 
     * @param request トークンリフレッシュリクエスト
     * @param http    HttpServletRequest（User-Agent, IP取得用）
     * @return 認証レスポンス（新アクセストークン・リフレッシュトークン）
     * @throws com.example.backend.exception.AuthException                  認証失敗時
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshRequestDto request,
            HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        String ip = resolveIp(http);
        AuthResponseDto response = authService.refresh(request.refreshToken(), ua, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * クライアントIPアドレスを解決。
     * <p>
     * X-Forwarded-For優先、なければremoteAddr。
     * </p>
     * 
     * @param http HttpServletRequest
     * @return クライアントIPアドレス
     */
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
