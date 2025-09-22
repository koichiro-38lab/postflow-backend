package com.example.backend.controller.admin;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.category.CategoryRequestDto;
import com.example.backend.repository.CategoryRepository;
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
class CategoryControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    UserRepository userRepository;

    private List<Long> createdCategoryIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long id : createdCategoryIds) {
            if (categoryRepository.existsById(id)) {
                categoryRepository.deleteById(id);
            }
        }
        createdCategoryIds.clear();
    }

    // ユーザー名とパスワードでログインしてアクセストークンを取得
    private String getAccessToken(String email, String password) throws Exception {
        String loginJson = objectMapper.writeValueAsString(new LoginRequestDto(email, password));
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    // 全カテゴリー取得
    @Test
    void getCategories_shouldReturn200() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");

        mockMvc.perform(get("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // IDでカテゴリー取得
    @Test
    void getById_existingCategory_shouldReturn200() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");

        // Create a category first
        String createJson = objectMapper.writeValueAsString(
                new CategoryRequestDto("Test Category", "test-category-" + System.currentTimeMillis(), null));
        String createResponse = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = Long.valueOf(JsonPath.read(createResponse, "$.id").toString());
        createdCategoryIds.add(categoryId);

        mockMvc.perform(get("/api/admin/categories/" + categoryId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Test Category"));
    }

    // 存在しないIDでカテゴリー取得
    @Test
    void getById_nonExistingCategory_shouldReturn404() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");

        mockMvc.perform(get("/api/admin/categories/99999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // カテゴリー作成
    @Test
    void createCategory_byAdmin_shouldReturn201() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");

        String json = objectMapper.writeValueAsString(
                new CategoryRequestDto("New Category", "new-category-" + System.currentTimeMillis(), null));
        String response = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = Long.valueOf(JsonPath.read(response, "$.id").toString());
        createdCategoryIds.add(categoryId);
    }

    // AUTHOR権限でカテゴリー作成しようとすると403
    @Test
    void createCategory_byAuthor_shouldReturn403() throws Exception {
        String authorToken = getAccessToken("author@example.com", "password123");

        String json = objectMapper.writeValueAsString(
                new CategoryRequestDto("New Category", "new-category-" + System.currentTimeMillis(), null));
        mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }

    // カテゴリー更新
    @Test
    void updateCategory_byAdmin_shouldReturn200() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");

        // カテゴリー作成
        String createJson = objectMapper.writeValueAsString(
                new CategoryRequestDto("Test Category", "test-category-" + System.currentTimeMillis(), null));
        String createResponse = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = Long.valueOf(JsonPath.read(createResponse, "$.id").toString());
        createdCategoryIds.add(categoryId);

        // カテゴリー更新
        String updateJson = objectMapper.writeValueAsString(
                new CategoryRequestDto("Updated Category", "updated-category-" + System.currentTimeMillis(), null));
        mockMvc.perform(put("/api/admin/categories/" + categoryId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Category"));
    }

    // AUTHOR権限でカテゴリー更新しようとすると403
    @Test
    void updateCategory_byAuthor_shouldReturn403() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        String authorToken = getAccessToken("author@example.com", "password123");
        String updateJson = objectMapper.writeValueAsString(
                new CategoryRequestDto("Updated Category", "updated-category-" + System.currentTimeMillis(), null));

        // カテゴリー作成
        String createJson = objectMapper.writeValueAsString(
                new CategoryRequestDto("Test Category", "test-category-" + System.currentTimeMillis(), null));
        String createResponse = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = Long.valueOf(JsonPath.read(createResponse, "$.id").toString());
        createdCategoryIds.add(categoryId);

        // カテゴリー更新
        mockMvc.perform(put("/api/admin/categories/" + categoryId)
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isForbidden());
    }

    // カテゴリー削除
    @Test
    void deleteCategory_byAdmin_shouldReturn204() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");

        // カテゴリー作成
        String createJson = objectMapper.writeValueAsString(
                new CategoryRequestDto("Test Category", "test-category-" + System.currentTimeMillis(), null));
        String createResponse = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = Long.valueOf(JsonPath.read(createResponse, "$.id").toString());
        createdCategoryIds.add(categoryId);

        // 削除
        mockMvc.perform(delete("/api/admin/categories/" + categoryId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        createdCategoryIds.remove(categoryId);
    }

    // AUTHOR権限でカテゴリー削除しようとすると403
    @Test
    void deleteCategory_byAuthor_shouldReturn403() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        String authorToken = getAccessToken("author@example.com", "password123");

        // カテゴリー作成
        String createJson = objectMapper.writeValueAsString(
                new CategoryRequestDto("Test Category", "test-category-" + System.currentTimeMillis(), null));
        String createResponse = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = Long.valueOf(JsonPath.read(createResponse, "$.id").toString());
        createdCategoryIds.add(categoryId);

        // 削除
        mockMvc.perform(delete("/api/admin/categories/" + categoryId)
                .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isForbidden());
    }
}