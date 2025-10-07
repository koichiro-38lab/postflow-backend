package com.example.backend.controller.admin;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.category.CategoryRequestDto;
import com.example.backend.dto.category.CategoryReorderRequestDto;
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

    // 全カテゴリー取得（作成したカテゴリを含むことを確認）
    @Test
    void getCategories_should_return_list_and_include_created() throws Exception {
        // adminでログインしてアクセストークンを取得
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // findAllで表示されるようにカテゴリを作成
        String expectedSlug = "integration-category-" + System.currentTimeMillis();
        var createReq = new CategoryRequestDto("Integration Category", expectedSlug, null);
        var createRes = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdCategoryIds.add(categoryId);

        // findAllを呼び出し
        mockMvc.perform(get("/api/admin/categories")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug=='" + expectedSlug + "')]").exists());
    }

    // カテゴリー並び替え（sortOrderの更新を確認）
    @Test
    void reorderCategories_should_update_sort_order() throws Exception {
        var loginReq = new LoginRequestDto("admin@example.com", "password123");
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginRes, "$.accessToken");

        // 並び替え用の2つのカテゴリを作成
        var req1 = new CategoryRequestDto("Reorder A", "reorder-a-" + System.currentTimeMillis(), null);
        var res1 = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id1 = Long.valueOf((Integer) JsonPath.read(res1, "$.id"));
        createdCategoryIds.add(id1);

        var req2 = new CategoryRequestDto("Reorder B", "reorder-b-" + System.currentTimeMillis(), null);
        var res2 = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id2 = Long.valueOf((Integer) JsonPath.read(res2, "$.id"));
        createdCategoryIds.add(id2);

        // 並び替えリクエスト送信: id1をsortOrder 10、id2を0に設定
        var reorderList = List.of(
                new CategoryReorderRequestDto(id1, 10),
                new CategoryReorderRequestDto(id2, 0));

        mockMvc.perform(put("/api/admin/categories/reorder")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reorderList)))
                .andExpect(status().isOk());

        // findAllで新しいsortOrderが反映されていることを確認（DTOにsortOrderフィールドを含む）
        mockMvc.perform(get("/api/admin/categories")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id1 + ")].sortOrder").value(org.hamcrest.Matchers.hasItem(10)))
                .andExpect(jsonPath("$[?(@.id==" + id2 + ")].sortOrder").value(org.hamcrest.Matchers.hasItem(0)));
    }
}