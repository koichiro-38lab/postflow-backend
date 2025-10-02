package com.example.backend.controller;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.user.UserProfileUpdateRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestClockConfig.class, TestDataConfig.class })
@org.springframework.test.context.ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // ========== プロフィール管理のテスト ==========

    // 自分のプロフィール取得成功は200
    @Test
    void getMyProfile_success_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // 自分のプロフィール更新成功は200
    @Test
    void updateMyProfile_success_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var updateReq = new UserProfileUpdateRequestDto(
                "Admin User",
                "System Administrator",
                null
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Admin User"))
                .andExpect(jsonPath("$.bio").value("System Administrator"));
    }

    // displayNameが100文字を超えるとバリデーションエラー
    @Test
    void updateMyProfile_displayNameTooLong_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        String longName = "a".repeat(101);
        var updateReq = new UserProfileUpdateRequestDto(
                longName,
                "Bio",
                null
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());
    }

    // bioが5000文字を超えるとバリデーションエラー
    @Test
    void updateMyProfile_bioTooLong_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        String longBio = "a".repeat(5001);
        var updateReq = new UserProfileUpdateRequestDto(
                "Name",
                longBio,
                null
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());
    }

    // 認証なしでプロフィール取得は403
    @Test
    void getMyProfile_noAuth_should_return_403() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    // 認証なしでプロフィール更新は403
    @Test
    void updateMyProfile_noAuth_should_return_403() throws Exception {
        var updateReq = new UserProfileUpdateRequestDto(
                "Name",
                "Bio",
                null
        );

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }
}
