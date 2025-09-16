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

    public String generateAccessToken(String subject, Collection<String> roles) {
        return generateToken(subject, roles, props.getAccessTtl().toSeconds());
    }

    public String generateRefreshToken(String subject) {
        return generateToken(subject, List.of(), props.getRefreshTtl().toSeconds());
    }

    public TokenPair issueTokens(String subject, Collection<String> roles) {
        String access = generateAccessToken(subject, roles);
        String refresh = generateRefreshToken(subject);
        return new TokenPair(access, refresh);
    }

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

    public record VerifiedToken(
            String subject,
            List<String> roles,
            Instant issuedAt,
            Instant expiresAt,
            Map<String, Object> claims) {
    }

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
