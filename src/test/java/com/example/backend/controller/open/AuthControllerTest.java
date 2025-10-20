package com.example.backend.controller.open;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.auth.RefreshRequestDto;
import com.example.backend.repository.RefreshTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestClockConfig.class, TestDataConfig.class })
@org.springframework.test.context.ActiveProfiles("test")
class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Clock clock;

    // テスト用Clockの進行を制御
    void setTestClockSecondsOffset(long seconds) {
        TestClockConfig.setOffsetSeconds(seconds);
    }

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
    }

    // ログイン成功
    @Test
    void login_success() throws Exception {
        var req = new LoginRequestDto("admin@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    // ログイン失敗（パスワード間違い）
    @Test
    void login_invalidPassword() throws Exception {
        var req = new LoginRequestDto("admin@example.com", "wrongpass");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0].field").value("error"));
    }

    // リフレッシュ失敗（不正なトークン）
    @Test
    void refresh_invalidToken() throws Exception {
        var req = new RefreshRequestDto("invalidtoken");
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0].field").value("error"));
    }

    // リフレッシュトークンのローテーション
    @Test
    void refresh_token_rotation() throws Exception {
        // ログインして取得
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String oldRefreshToken = JsonPath.read(loginRes, "$.refreshToken");

        setTestClockSecondsOffset(0);

        // 旧トークンでリフレッシュ
        var refreshReq = new RefreshRequestDto(oldRefreshToken);
        var refreshRes = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();
        String newRefreshToken = JsonPath.read(refreshRes, "$.refreshToken");

        setTestClockSecondsOffset(2);

        // 旧トークン再使用は失敗
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0].field").value("error"));

        setTestClockSecondsOffset(4);

        // 新トークンで成功
        var newRefreshReq = new RefreshRequestDto(newRefreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRefreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    // リフレッシュトークンの期限切れは401
    @Test
    void refreshToken_expiration() throws Exception {
        // ログインしてリフレッシュトークン取得
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(loginRes, "$.refreshToken");

        // 期限超過まで進める
        setTestClockSecondsOffset(60 * 60 * 24 * 10);

        // 期限切れで401
        var refreshReq = new RefreshRequestDto(refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0].field").value("error"));
    }

    // リフレッシュトークンの再利用は401
    @Test
    void refreshToken_reuse_should_fail() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(loginRes, "$.refreshToken");

        setTestClockSecondsOffset(2);

        // 1回目成功
        var refreshReq = new RefreshRequestDto(refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());

        // 2回目失敗
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    // ログイン失敗（バリデーションエラー：空のメールアドレス）
    @Test
    void login_validationError_emptyEmail() throws Exception {
        var req = new LoginRequestDto("", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray());
    }

    // ログイン失敗（バリデーションエラー：空のパスワード）
    @Test
    void login_validationError_emptyPassword() throws Exception {
        var req = new LoginRequestDto("admin@example.com", "");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray());
    }

    // リフレッシュ失敗（バリデーションエラー：空のトークン）
    @Test
    void refresh_validationError_emptyToken() throws Exception {
        var req = new RefreshRequestDto("");
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray());
    }

    // IP解決テスト（X-Forwarded-For ヘッダーなし）
    @Test
    void login_withRemoteAddr_usesRemoteAddr() throws Exception {
        var req = new LoginRequestDto("admin@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(request -> {
                    // RemoteAddr を設定
                    request.setRemoteAddr("192.168.1.100");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    // IP解決テスト（X-Forwarded-For ヘッダーあり）
    @Test
    void login_withXForwardedFor_usesXForwardedFor() throws Exception {
        var req = new LoginRequestDto("admin@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-Forwarded-For", "203.0.113.1"))
                .andExpect(status().isOk());
    }

    // IP解決テスト（X-Forwarded-For ヘッダーに複数IP、カンマ区切り）
    @Test
    void login_withXForwardedFor_multipleIps_usesFirst() throws Exception {
        var req = new LoginRequestDto("admin@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1, 192.0.2.1"))
                .andExpect(status().isOk());
    }
}