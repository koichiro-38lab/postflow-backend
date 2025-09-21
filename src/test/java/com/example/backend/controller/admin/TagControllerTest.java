package com.example.backend.controller.admin;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.tag.TagRequestDto;
import com.example.backend.entity.Tag;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestClockConfig.class, TestDataConfig.class })
@org.springframework.test.context.ActiveProfiles("test")
class TagControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    UserRepository userRepository;

    private final List<Long> createdTagIds = new ArrayList<>();
    private final List<Long> createdPostIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        // 参照整合性のため先にポストを削除
        createdPostIds.forEach(postRepository::deleteById);
        createdPostIds.clear();
        createdTagIds.forEach(tagRepository::deleteById);
        createdTagIds.clear();
    }

    // Tag作成 (ADMIN)
    @Test
    void createTag_byAdmin_shouldReturn201() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        var request = TagRequestDto.builder().name("x-test-newtag").slug("x-test-newtag").build();

        String response = mockMvc.perform(post("/api/admin/tags")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("x-test-newtag"))
                .andExpect(jsonPath("$.slug").value("x-test-newtag"))
                .andReturn().getResponse().getContentAsString();

        Long tagId = Long.valueOf((Integer) JsonPath.read(response, "$.id"));
        createdTagIds.add(tagId);
    }

    // Tag作成 (EDITOR)
    @Test
    void createTag_byEditor_shouldReturn201() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");
        var request = TagRequestDto.builder().name("x-test-newtag").slug("x-test-newtag").build();

        String response = mockMvc.perform(post("/api/admin/tags")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("x-test-newtag"))
                .andExpect(jsonPath("$.slug").value("x-test-newtag"))
                .andReturn().getResponse().getContentAsString();

        Long tagId = Long.valueOf((Integer) JsonPath.read(response, "$.id"));
        createdTagIds.add(tagId);
    }

    // Duplicate nameでのTag作成失敗
    @Test
    void createTag_withDuplicateName_shouldReturn409() throws Exception {
        // ensure a clean unique baseline for duplicate scenario
        Tag existing = tagRepository.save(Tag.builder().name("x-test-dup").slug("x-test-dup").build());
        createdTagIds.add(existing.getId());

        String adminToken = getAccessToken("admin@example.com", "password123");
        var request = TagRequestDto.builder().name("x-test-dup").slug("x-test-dup").build();

        mockMvc.perform(post("/api/admin/tags")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // Author権限でのTag作成失敗
    @Test
    void createTag_byAuthor_shouldReturn403() throws Exception {
        String authorToken = getAccessToken("author@example.com", "password123");
        var request = TagRequestDto.builder().name("x-test-newtag").slug("x-test-newtag").build();

        mockMvc.perform(post("/api/admin/tags")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // Author権限でのTag一覧取得成功
    @Test
    void listTags_byAuthor_shouldReturn200() throws Exception {
        Tag tag1 = tagRepository.save(Tag.builder().name("x-test-spring").slug("x-test-spring").build());
        Tag tag2 = tagRepository.save(Tag.builder().name("x-test-java").slug("x-test-java").build());
        createdTagIds.add(tag1.getId());
        createdTagIds.add(tag2.getId());

        String authorToken = getAccessToken("author@example.com", "password123");

        mockMvc.perform(get("/api/admin/tags")
                .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'x-test-spring')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'x-test-java')]").exists());
    }

    // Tag更新 (EDITOR)
    @Test
    void updateTag_byEditor_shouldReturn200() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("x-test-backend").slug("x-test-backend").build());
        createdTagIds.add(tag.getId());

        String editorToken = getAccessToken("editor@example.com", "password123");
        var request = TagRequestDto.builder().name("x-test-backend-arch").slug("x-test-backend-arch").build();

        mockMvc.perform(put("/api/admin/tags/" + tag.getId())
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("x-test-backend-arch"));
    }

    // Tag削除 (ADMIN)
    @Test
    void deleteTag_byAdmin_shouldReturn204() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("x-test-cloud").slug("x-test-cloud").build());
        createdTagIds.add(tag.getId());

        String adminToken = getAccessToken("admin@example.com", "password123");

        mockMvc.perform(delete("/api/admin/tags/" + tag.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        createdTagIds.remove(tag.getId());
        assert tagRepository.findById(tag.getId()).isEmpty();
    }

    // Tag削除失敗 (紐づくPostが存在する場合)
    @Test
    void deleteTag_inUse_shouldReturn409() throws Exception {
        // タグ作成
        Tag tag = tagRepository.save(Tag.builder().name("x-test-inuse").slug("x-test-inuse").build());
        createdTagIds.add(tag.getId());

        // 投稿を関連付け (adminユーザをauthorとして利用)
        User author = userRepository.findByEmail("admin@example.com").orElseThrow();
        long ts = System.currentTimeMillis();
        Post post = postRepository.save(Post.builder()
                .title("x-test-post-inuse-" + ts)
                .slug("x-test-post-inuse-" + ts)
                .status(Post.Status.DRAFT)
                .excerpt("test")
                .contentJson("{}")
                .author(author)
                .tags(List.of(tag))
                .build());
        createdPostIds.add(post.getId());

        String adminToken = getAccessToken("admin@example.com", "password123");

        mockMvc.perform(delete("/api/admin/tags/" + tag.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    // ユーザ認証してアクセストークンを取得
    private String getAccessToken(String email, String password) throws Exception {
        var loginReq = new LoginRequestDto(email, password);
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(loginRes, "$.accessToken");
    }
}
