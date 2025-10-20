package com.example.backend.service;

import com.example.backend.config.JwtProperties;
import com.example.backend.dto.auth.AuthResponseDto;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.entity.RefreshToken;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.exception.AccountDisabledException;
import com.example.backend.exception.InvalidRefreshTokenException;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.JwtTokenService.TokenPair;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.example.backend.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 認証サービス。
 * <p>
 * ログイン・リフレッシュトークン発行・トークンローテーション・ハッシュ化等を提供。
 * <ul>
 * <li>ログイン: メール・パスワード認証、JWT発行、リフレッシュトークン保存</li>
 * <li>リフレッシュ: 有効なリフレッシュトークンでアクセストークン再発行、旧トークン失効</li>
 * <li>ハッシュ化: トークンのSHA-256ハッシュ化</li>
 * </ul>
 * 
 * @see com.example.backend.service.JwtTokenService
 * @see com.example.backend.repository.RefreshTokenRepository
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final java.time.Clock clock;

    /**
     * ログイン処理。
     * <p>
     * メール・パスワード認証し、JWTアクセストークン・リフレッシュトークンを発行。ユーザーがACTIVEでない場合は例外。
     * </p>
     * 
     * @param request   ログインリクエスト
     * @param userAgent ユーザーエージェント
     * @param ipAddress IPアドレス
     * @return 署名済みのアクセストークン・リフレッシュトークンDTO
     * @throws com.example.backend.exception.InvalidCredentialsException 認証失敗時
     * @throws com.example.backend.exception.AccountDisabledException    アカウント無効時
     */
    @Transactional
    public AuthResponseDto login(LoginRequestDto request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // ステータスチェック: ACTIVE のユーザーのみログイン可能
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountDisabledException("Account is disabled. Please contact an administrator.");
        }

        // ログイン成功時に最終ログイン日時を更新
        user.setLastLoginAt(LocalDateTime.now(clock));
        userRepository.save(user);

        TokenPair pair = jwtTokenService.issueTokens(user.getEmail(), List.of(user.getRole().name()));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(pair.refreshToken()))
                .issuedAt(clock.instant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .expiresAt(clock.instant().plus(jwtProperties.getRefreshTtl()).atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(refreshToken);

        Duration accessTtl = jwtProperties.getAccessTtl();
        long expiresIn = accessTtl.toSeconds();

        return new AuthResponseDto(pair.accessToken(), pair.refreshToken(), "Bearer", expiresIn);
    }

    /**
     * リフレッシュトークンによるトークン再発行。
     * <p>
     * 有効なリフレッシュトークンを検証し、アクセストークン・リフレッシュトークンを再発行。旧トークンは失効。ユーザーがACTIVEでない場合は例外。
     * </p>
     * 
     * @param refreshTokenRaw 元のリフレッシュトークン文字列
     * @param userAgent       ユーザーエージェント
     * @param ipAddress       IPアドレス
     * @return 署名済みの新しいアクセストークン・リフレッシュトークンDTO
     * @throws com.example.backend.exception.InvalidRefreshTokenException トークン不正・期限切れ・失効時
     * @throws com.example.backend.exception.AccountDisabledException     アカウント無効時
     */
    @Transactional
    public AuthResponseDto refresh(String refreshTokenRaw, String userAgent, String ipAddress) {
        String hash = sha256Hex(refreshTokenRaw);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        // 失効・期限切れは最初に判定し、例外時は以降の処理を行わない
        if (token.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException("Refresh token revoked");
        }
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // 署名/有効期限の検証
        var verified = jwtTokenService.verify(refreshTokenRaw);

        // 発行者の整合性チェック（任意: subject == user.email）
        if (verified.subject() == null || !verified.subject().equals(token.getUser().getEmail())) {
            throw new InvalidRefreshTokenException("Refresh token subject mismatch");
        }

        User user = token.getUser();

        // ステータスチェック: ACTIVE のユーザーのみトークンリフレッシュ可能
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountDisabledException("Account is disabled. Please contact an administrator.");
        }

        // ローテーション: 旧トークンは失効
        System.out.println("DEBUG: Revoking old token with ID: " + token.getId());
        token.setRevokedAt(LocalDateTime.now(clock));
        refreshTokenRepository.save(token);
        var roles = List.of(user.getRole().name());
        var pair = jwtTokenService.issueTokens(user.getEmail(), roles);

        System.out.println("DEBUG: Generated new refresh token: " + pair.refreshToken().substring(0, 20) + "...");
        System.out.println("DEBUG: New token hash: " + sha256Hex(pair.refreshToken()));

        var verifiedNew = jwtTokenService.verify(pair.refreshToken());
        var newEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(pair.refreshToken()))
                .expiresAt(LocalDateTime.ofInstant(verifiedNew.expiresAt(), java.time.ZoneOffset.UTC))
                .issuedAt(clock.instant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        RefreshToken savedToken = refreshTokenRepository.save(newEntity);
        System.out.println("DEBUG: Saved new token with ID: " + savedToken.getId());

        long expiresIn = jwtProperties.getAccessTtl().toSeconds();
        System.out.println("DEBUG: AuthService.refresh() END");
        return new AuthResponseDto(pair.accessToken(), pair.refreshToken(), "Bearer", expiresIn);
    }

    /**
     * SHA-256ハッシュを計算。
     * <p>
     * トークン等のハッシュ化に利用。
     * </p>
     * 
     * @param value 入力文字列
     * @return SHA-256ハッシュの16進文字列
     * @throws IllegalStateException SHA-256アルゴリズムが利用できない場合
     */
    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
