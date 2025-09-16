package com.example.backend.controller.admin;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.user.UserRequestDto;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestClockConfig.class, TestDataConfig.class })
@org.springframework.test.context.ActiveProfiles("test")
class UserAdminControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    private List<Long> createdUserIds = new ArrayList<>();

    void setTestClockSecondsOffset(long seconds) {
        TestClockConfig.setOffsetSeconds(seconds);
    }

    @AfterEach
    void tearDown() {
        // テストで作成したユーザーをクリーンアップ
        createdUserIds.forEach(userRepository::deleteById);
        createdUserIds.clear();
    }

    // メールアドレス未入力でのユーザー登録は400
    @Test
    void createUser_blankEmail_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("", "password123", "AUTHOR");
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("email"))
                .andExpect(jsonPath("$.messages[0].code").value("error.email.required"));
    }

    // メールアドレス形式不正でのユーザー登録は400
    @Test
    void createUser_invalidEmailFormat_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("invalid-email-format", "password123", "AUTHOR");
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("email"))
                .andExpect(jsonPath("$.messages[0].code").value("error.email.invalid"));
    }

    // メールアドレス重複でのユーザー登録は409
    @Test
    void createUser_duplicateEmail_should_return_409() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("test@example.com", "password123", "AUTHOR");
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0].field").value("error"))
                .andExpect(jsonPath("$.messages[0].code").value("error.email.duplicate"));
    }

    // メールアドレスが短い場合のユーザー登録は400
    @Test
    void createUser_shortEmail_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("short", "password123", "AUTHOR");
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("email"))
                .andExpect(jsonPath("$.messages[0].code").value("error.email.invalid"));
    }

    // メールアドレスにSQLインジェクション攻撃コードが含まれる場合のユーザー登録は400
    @Test
    void createUser_sqlInjectionInEmail_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("test@example.com' OR '1'='1", "password123", "AUTHOR");
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("email"))
                .andExpect(jsonPath("$.messages[0].code").value("error.email.invalid"));
    }

    // メールアドレスにXSS攻撃コードが含まれる場合のユーザー登録は400
    @Test
    void createUser_xssInEmail_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("<script>alert('xss')</script>", "password123", "AUTHOR");
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("email"))
                .andExpect(jsonPath("$.messages[0].code").value("error.email.invalid"));
    }

    // 不正なロールでのユーザー登録は400
    @Test
    void createUser_invalidRole_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 不正なロールでのユーザー登録
        var createUserReq = new UserRequestDto("newuser" + System.currentTimeMillis() + "@example.com", "password123",
                "INVALID_ROLE");
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("error"));
    }

    // 管理者ユーザーで /api/admin/users にアクセスして200
    @Test
    void admin_users_should_return_200_after_login() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    // 権限のないユーザーで /api/admin/users にアクセスして403
    @Test
    void admin_users_should_return_403_forbidden_for_non_admin_user() throws Exception {
        var loginReq = new LoginRequestDto("author@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages[0].field").value("error"));
    }

    // 存在しないユーザーの更新は404
    @Test
    void updateUser_notFound_should_return_404() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var updateUserReq = new UserRequestDto("nonexistent@example.com", "password123", "AUTHOR");
        mockMvc.perform(put("/api/admin/users/99999")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0].field").value("error"));
    }

    // ユーザー作成成功は201
    @Test
    void createUser_success_should_return_201() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        String uniqueEmail = "newuser" + System.currentTimeMillis() + "@example.com";
        var req = new UserRequestDto(uniqueEmail, "password123", "AUTHOR");
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);
    }

    // ユーザー更新成功は200
    @Test
    void updateUser_success_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // まず作成
        String createEmail = "updateuser" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(createEmail, "password123", "AUTHOR");
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // 更新
        String updatedEmail = "updated" + System.currentTimeMillis() + "@example.com";
        var updateReq = new UserRequestDto(updatedEmail, "newpassword123", "EDITOR");
        mockMvc.perform(put("/api/admin/users/" + userId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(updatedEmail));
    }

    // ユーザー削除成功は204
    @Test
    void deleteUser_success_should_return_204() throws Exception {
        var loginReq = new LoginRequestDto("test@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String deleteEmail = "deleteuser" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(deleteEmail, "password123", "AUTHOR");
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // 削除
        mockMvc.perform(delete("/api/admin/users/" + userId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

}
