package com.example.backend.service;

import com.example.backend.config.JwtProperties;
import com.example.backend.dto.auth.AuthResponseDto;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.entity.RefreshToken;
import com.example.backend.entity.User;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.exception.InvalidRefreshTokenException;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final java.time.Clock clock;

    @Transactional
    public AuthResponseDto login(LoginRequestDto request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        var roles = List.of(user.getRole().name());
        var pair = jwtTokenService.issueTokens(user.getEmail(), roles);

        // 保存: refresh token のハッシュとメタ
        var verified = jwtTokenService.verify(pair.refreshToken());
        var entity = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(pair.refreshToken()))
                .expiresAt(LocalDateTime.ofInstant(verified.expiresAt(), java.time.ZoneOffset.UTC))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(entity);

        long expiresIn = jwtProperties.getAccessTtl().toSeconds();
        return new AuthResponseDto(pair.accessToken(), pair.refreshToken(), "Bearer", expiresIn);
    }

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

        // ローテーション: 旧トークンは失効
        token.setRevokedAt(LocalDateTime.now(clock));
        refreshTokenRepository.save(token);

        User user = token.getUser();
        var roles = List.of(user.getRole().name());
        var pair = jwtTokenService.issueTokens(user.getEmail(), roles);

        var verifiedNew = jwtTokenService.verify(pair.refreshToken());
        var newEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(pair.refreshToken()))
                .expiresAt(LocalDateTime.ofInstant(verifiedNew.expiresAt(), java.time.ZoneOffset.UTC))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(newEntity);

        long expiresIn = jwtProperties.getAccessTtl().toSeconds();
        return new AuthResponseDto(pair.accessToken(), pair.refreshToken(), "Bearer", expiresIn);
    }

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
