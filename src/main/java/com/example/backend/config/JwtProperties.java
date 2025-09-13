package com.example.backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /**
     * HS256 用のシークレット（32byte 以上を推奨。base64 文字列も可）
     */
    private String secret;

    /**
     * アクセストークンの有効期間（例: 15m, 1h）
     */
    private Duration accessTtl = Duration.ofMinutes(15);

    /**
     * リフレッシュトークンの有効期間（例: 7d）
     */
    private Duration refreshTtl = Duration.ofDays(7);

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public Duration getAccessTtl() { return accessTtl; }
    public void setAccessTtl(Duration accessTtl) { this.accessTtl = accessTtl; }

    public Duration getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(Duration refreshTtl) { this.refreshTtl = refreshTtl; }
}

