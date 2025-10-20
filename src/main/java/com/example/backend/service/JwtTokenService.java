package com.example.backend.service;

import com.example.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Clock;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * JWTトークン発行・検証サービス。
 * <p>
 * アクセストークン・リフレッシュトークンの発行、検証、署名キー管理を提供。
 * <ul>
 * <li>トークン発行: subject/roles/有効期限付きJWT生成</li>
 * <li>検証: 署名・有効期限・クレーム検証</li>
 * <li>署名キー: HS256固定、キャッシュ付き</li>
 * </ul>
 * 
 * @see com.example.backend.config.JwtProperties
 */
@Service
public class JwtTokenService {
    public static final String ROLES_CLAIM = "roles";

    private final JwtProperties props;
    private final Clock clock;
    private volatile SecretKey cachedKey; // 再計算を避ける簡易キャッシュ

    public JwtTokenService(JwtProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    /**
     * アクセストークンを発行。
     * <p>
     * subject/roles/有効期限付きのJWTをHS256署名で生成。
     * </p>
     * 
     * @param subject トークンのサブジェクト
     * @param roles   ユーザーのロール
     * @return 署名済みのアクセストークン文字列
     */
    public String generateAccessToken(String subject, Collection<String> roles) {
        return generateToken(subject, roles, props.getAccessTtl().toSeconds());
    }

    /**
     * リフレッシュトークンを発行。
     * <p>
     * subject/有効期限付きのJWTをHS256署名で生成。
     * </p>
     * 
     * @param subject トークンのサブジェクト
     * @return 署名済みのリフレッシュトークン文字列
     */
    public String generateRefreshToken(String subject) {
        return generateToken(subject, List.of(), props.getRefreshTtl().toSeconds());
    }

    /**
     * アクセストークン・リフレッシュトークンのペアを発行。
     * <p>
     * subject/rolesを元に両トークンを同時発行。
     * </p>
     * 
     * @param subject トークンのサブジェクト
     * @param roles   ユーザーのロール
     * @return 署名済みのアクセストークン・リフレッシュトークンペア
     */
    public TokenPair issueTokens(String subject, Collection<String> roles) {
        String access = generateAccessToken(subject, roles);
        String refresh = generateRefreshToken(subject);
        return new TokenPair(access, refresh);
    }

    /**
     * トークンを検証・解析。
     * <p>
     * 署名・有効期限・クレームを検証し、VerifiedTokenを返却。
     * </p>
     * 
     * @param token 署名済みのトークン文字列
     * @return 検証済みトークン情報
     * @throws io.jsonwebtoken.security.SecurityException 署名不正時
     * @throws io.jsonwebtoken.ExpiredJwtException        有効期限切れ時
     */
    public VerifiedToken verify(String token) {
        Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
        Claims claims = claimsJws.getPayload();
        String subject = claims.getSubject();
        List<String> roles = null;
        Object raw = claims.get(ROLES_CLAIM);
        if (raw instanceof List<?> list) {
            roles = list.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null;
        Instant expiresAt = claims.getExpiration() != null ? claims.getExpiration().toInstant() : null;
        return new VerifiedToken(subject, roles, issuedAt, expiresAt, claims);
    }

    /**
     * 検証済みトークン情報。
     * <p>
     * subject/roles/発行・有効期限/全クレームを保持。
     * </p>
     */
    public record VerifiedToken(
            String subject,
            List<String> roles,
            Instant issuedAt,
            Instant expiresAt,
            Map<String, Object> claims) {
    }

    /**
     * トークンを生成。
     * <p>
     * subject/roles/有効期限付きJWTをHS256署名で生成。
     * </p>
     * 
     * @param subject    トークンのサブジェクト
     * @param roles      ユーザーのロール
     * @param ttlSeconds 有効期限（秒）
     * @return 署名済みのトークン文字列
     */
    private String generateToken(String subject, Collection<String> roles, long ttlSeconds) {
        var now = Instant.now(clock);
        var exp = now.plusSeconds(ttlSeconds);
        var builder = Jwts.builder()
                .subject(subject)
                // 一意なJWT IDを付与し、同一時刻発行でもトークンが重複しないようにする
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));
        if (roles != null && !roles.isEmpty()) {
            builder.claim(ROLES_CLAIM, roles);
        }
        return builder
                // Resource Server 側 (Nimbus) は HS256 を期待するため、署名も HS256 に固定
                .signWith(signingKey(), io.jsonwebtoken.Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 署名キーを取得（キャッシュ付き）。
     * <p>
     * JWTのHS256署名に利用。app.jwt.secretが未設定の場合は例外。
     * </p>
     * 
     * @return 署名用SecretKey
     * @throws IllegalStateException シークレット未設定時
     */
    private SecretKey signingKey() {
        SecretKey key = cachedKey;
        if (key != null)
            return key;
        String secret = props.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not set (app.jwt.secret)");
        }
        // SecurityConfig 側の NimbusJwtDecoder も UTF-8 バイト列をそのまま使っているため、こちらも合わせる
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        key = Keys.hmacShaKeyFor(bytes);
        cachedKey = key;
        return key;
    }
}
