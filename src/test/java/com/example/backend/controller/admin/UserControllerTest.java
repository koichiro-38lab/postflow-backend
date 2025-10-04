package com.example.backend.controller.admin;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.user.UserRequestDto;
import com.example.backend.dto.user.UserUpdateRequestDto;
import com.example.backend.entity.UserStatus;
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
class UserControllerTest {
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("", "password123", "AUTHOR", "Test User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("invalid-email-format", "password123", "AUTHOR", "Test User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("admin@example.com", "password123", "ADMIN", "Test Admin", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("short", "password123", "AUTHOR", "Test User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("admin@example.com' OR '1'='1", "password123", "AUTHOR", "Test User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var req = new UserRequestDto("<script>alert('xss')</script>", "password123", "AUTHOR", "Test User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 不正なロールでのユーザー登録
        var createUserReq = new UserRequestDto("newuser" + System.currentTimeMillis() + "@example.com", "password123",
                "INVALID_ROLE", "Test User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
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
        var loginRes = mockMvc.perform(post("/api/auth/login")
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        var updateUserReq = new UserRequestDto("nonexistent@example.com", "password123", "AUTHOR", "Test User", null,
                null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        String uniqueEmail = "newuser" + System.currentTimeMillis() + "@example.com";
        var req = new UserRequestDto(uniqueEmail, "password123", "AUTHOR", "Test User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // まず作成
        String createEmail = "updateuser" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(createEmail, "password123", "AUTHOR", "Test User", null, null);
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
        var updateReq = new UserRequestDto(updatedEmail, "newpassword123", "EDITOR", "Updated User", null, null);
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
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String deleteEmail = "deleteuser" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(deleteEmail, "password123", "AUTHOR", "Test User", null, null);
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

    // ========== ユーザー管理機能のテスト ==========

    // ユーザー一覧取得（ページング）成功は200
    @Test
    void getAllUsers_withPagination_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        mockMvc.perform(get("/api/admin/users?page=0&size=10")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    // ユーザー一覧取得（ステータスフィルタ）成功は200
    @Test
    void getAllUsers_withStatusFilter_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        mockMvc.perform(get("/api/admin/users?status=ACTIVE")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ユーザーステータス変更成功は200
    @Test
    void updateUserStatus_success_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String testEmail = "statususer" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(testEmail, "password123", "AUTHOR", "Test User", null, null);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // ステータス変更
        var statusReq = "{\"status\": \"INACTIVE\"}";
        mockMvc.perform(patch("/api/admin/users/" + userId + "/status")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    // 自分自身のステータス変更は403
    @Test
    void updateUserStatus_self_should_return_403() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // admin ユーザーの ID を取得（TestDataConfig で id=1）
        Long adminId = 1L;

        // 自分自身のステータス変更を試みる
        var statusReq = "{\"status\": \"INACTIVE\"}";
        mockMvc.perform(patch("/api/admin/users/" + adminId + "/status")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusReq))
                .andExpect(status().isForbidden());
    }

    // ユーザーロール変更成功は200
    @Test
    void updateUserRole_success_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String testEmail = "roleuser" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(testEmail, "password123", "AUTHOR", "Test User", null, null);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // ロール変更
        var roleReq = "{\"role\": \"EDITOR\"}";
        mockMvc.perform(patch("/api/admin/users/" + userId + "/role")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    // 自分自身のロール変更は403
    @Test
    void updateUserRole_self_should_return_403() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // admin ユーザーの ID を取得（TestDataConfig で id=1）
        Long adminId = 1L;

        // 自分自身のロール変更を試みる
        var roleReq = "{\"role\": \"AUTHOR\"}";
        mockMvc.perform(patch("/api/admin/users/" + adminId + "/role")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleReq))
                .andExpect(status().isForbidden());
    }

    // ADMIN以外はユーザー管理機能にアクセスできない
    @Test
    void getAllUsers_nonAdmin_should_return_403() throws Exception {
        var loginReq = new LoginRequestDto("author@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    // ========== UserUpdateRequestDto を使った包括的な更新のテスト ==========

    // 管理者によるユーザー包括的更新成功は200
    @Test
    void updateUserByAdmin_comprehensive_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // まず作成
        String createEmail = "comprehensive" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(createEmail, "password123", "AUTHOR", "Test User", null, null);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // 包括的な更新 (email, displayName, bio, role, status)
        String updatedEmail = "updated-comprehensive" + System.currentTimeMillis() + "@example.com";
        var updateReq = new UserUpdateRequestDto(
                updatedEmail,
                null, // password は更新しない
                "Updated Display Name",
                "Updated bio for comprehensive test",
                null, // avatarMediaId
                "EDITOR",
                UserStatus.INACTIVE);
        mockMvc.perform(put("/api/admin/users/" + userId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(updatedEmail))
                .andExpect(jsonPath("$.displayName").value("Updated Display Name"))
                .andExpect(jsonPath("$.bio").value("Updated bio for comprehensive test"))
                .andExpect(jsonPath("$.role").value("EDITOR"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    // displayNameのみ更新は200
    @Test
    void updateUserByAdmin_displayNameOnly_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String createEmail = "displaynametest" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(createEmail, "password123", "AUTHOR", "Test User", null, null);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // displayName のみ更新
        var updateReq = new UserUpdateRequestDto(
                null, // email
                null, // password
                "New Display Name",
                null, // bio
                null, // avatarMediaId
                null, // role
                null // status
        );
        mockMvc.perform(put("/api/admin/users/" + userId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.displayName").value("New Display Name"));
    }

    // ステータスとロールを同時に更新は200
    @Test
    void updateUserByAdmin_statusAndRole_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String createEmail = "statusroletest" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(createEmail, "password123", "AUTHOR", "Test User", null, null);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // status と role を同時に更新
        var updateReq = new UserUpdateRequestDto(
                null, // email
                null, // password
                null, // displayName
                null, // bio
                null, // avatarMediaId
                "EDITOR",
                UserStatus.SUSPENDED);
        mockMvc.perform(put("/api/admin/users/" + userId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.role").value("EDITOR"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    // パスワード更新は200
    @Test
    void updateUserByAdmin_password_should_return_200() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String createEmail = "passwordtest" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(createEmail, "password123", "AUTHOR", "Test User", null, null);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // パスワードのみ更新
        var updateReq = new UserUpdateRequestDto(
                null, // email
                "newpassword456",
                null, // displayName
                null, // bio
                null, // avatarMediaId
                null, // role
                null // status
        );
        mockMvc.perform(put("/api/admin/users/" + userId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));

        // 新しいパスワードでログイン可能か確認
        var newLoginReq = new LoginRequestDto(createEmail, "newpassword456");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    // メールアドレス重複での包括的更新は409
    @Test
    void updateUserByAdmin_duplicateEmail_should_return_409() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 1つ目のユーザー作成
        String createEmail1 = "dupemail1-" + System.currentTimeMillis() + "@example.com";
        var createReq1 = new UserRequestDto(createEmail1, "password123", "AUTHOR", "Test User 1", null, null);
        var createRes1 = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId1 = Long.valueOf((Integer) JsonPath.read(createRes1, "$.id"));
        createdUserIds.add(userId1);

        // 2つ目のユーザー作成
        String createEmail2 = "dupemail2-" + System.currentTimeMillis() + "@example.com";
        var createReq2 = new UserRequestDto(createEmail2, "password123", "AUTHOR", "Test User 2", null, null);
        var createRes2 = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId2 = Long.valueOf((Integer) JsonPath.read(createRes2, "$.id"));
        createdUserIds.add(userId2);

        // 2つ目のユーザーのメールを1つ目のメールと同じに更新しようとする
        var updateReq = new UserUpdateRequestDto(
                createEmail1, // 重複するメール
                null,
                null,
                null,
                null,
                null,
                null);
        mockMvc.perform(put("/api/admin/users/" + userId2)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0].code").value("error.email.duplicate"));
    }

    // 不正なロールでの包括的更新は400
    @Test
    void updateUserByAdmin_invalidRole_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 作成
        String createEmail = "invalidroletest" + System.currentTimeMillis() + "@example.com";
        var createReq = new UserRequestDto(createEmail, "password123", "AUTHOR", "Test User", null, null);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);

        // 不正なロールで更新
        var updateReq = new UserUpdateRequestDto(
                null,
                null,
                null,
                null,
                null,
                "INVALID_ROLE", // 不正なロール
                null);
        mockMvc.perform(put("/api/admin/users/" + userId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("error"));
    }

    // bioが5000文字を超える場合のユーザー登録は400
    @Test
    void createUser_bioTooLong_should_return_400() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        String longBio = "a".repeat(5001); // 5001文字
        var req = new UserRequestDto("test@example.com", "password123", "AUTHOR", "Test User", longBio, null);
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("bio"))
                .andExpect(jsonPath("$.messages[0].code").value("error.bio.tooLong"));
    }

    // bioとavatarMediaIdを同時に設定した正常作成は201
    @Test
    void createUser_withBioAndAvatar_should_return_201() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        String uniqueEmail = "withbio" + System.currentTimeMillis() + "@example.com";
        var req = new UserRequestDto(uniqueEmail, "password123", "AUTHOR", "Test User", "This is a test bio", 1L);
        var createRes = mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bio").value("This is a test bio"))
                .andExpect(jsonPath("$.avatarMediaId").value(1))
                .andReturn().getResponse().getContentAsString();
        Long userId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdUserIds.add(userId);
    }
}