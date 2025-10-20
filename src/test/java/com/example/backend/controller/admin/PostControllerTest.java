package com.example.backend.controller.admin;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.post.PostRequestDto;
import com.example.backend.entity.Post;
import com.example.backend.entity.Tag;
import com.example.backend.repository.PostRepository;
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

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestClockConfig.class, TestDataConfig.class })
@org.springframework.test.context.ActiveProfiles("test")
@Transactional
class PostControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PostRepository postRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    com.example.backend.repository.MediaRepository mediaRepository;
    @Autowired
    com.example.backend.repository.TagRepository tagRepository;

    private List<Long> createdPostIds = new ArrayList<>();
    private List<Long> createdMediaIds = new ArrayList<>();
    private List<Long> createdTagIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdPostIds.forEach(postRepository::deleteById);
        createdPostIds.clear();
        createdMediaIds.forEach(mediaRepository::deleteById);
        createdMediaIds.clear();
        createdTagIds.forEach(tagRepository::deleteById);
        createdTagIds.clear();
    }

    @Test
    void createPost_withCoverMedia_shouldReturnCoverInfo() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");
        var editor = userRepository.findByEmail("editor@example.com").orElseThrow();

        var media = mediaRepository.save(com.example.backend.entity.Media.builder()
                .filename("cover-image.png")
                .storageKey("media/test/" + java.util.UUID.randomUUID())
                .mime("image/png")
                .bytes(2048L)
                .width(800)
                .height(600)
                .altText("Cover")
                .createdBy(editor)
                .build());
        createdMediaIds.add(media.getId());

        var req = PostRequestDto.builder()
                .title("Cover Post")
                .slug("cover-post-slug")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"body\"}]}")
                .authorId(editor.getId())
                .coverMediaId(media.getId())
                .build();

        String res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverMedia.id").value(media.getId()))
                .andExpect(jsonPath("$.coverMedia.mime").value("image/png"))
                .andExpect(jsonPath("$.coverMedia.bytes").value(2048))
                .andReturn().getResponse().getContentAsString();

        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
        createdPostIds.add(postId);
    }

    // 投稿にタグを付与して作成
    @Test
    void createPost_withTags_shouldReturnTags() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");
        var editor = userRepository.findByEmail("editor@example.com").orElseThrow();

        Tag backend = tagRepository.save(Tag.builder().name("x-test-backend").slug("x-test-backend").build());
        Tag spring = tagRepository.save(Tag.builder().name("x-test-spring").slug("x-test-spring").build());
        createdTagIds.add(backend.getId());
        createdTagIds.add(spring.getId());

        var req = PostRequestDto.builder()
                .title("Tagged Post")
                .slug("tagged-post")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"body\"}]}")
                .authorId(editor.getId())
                .tags(List.of("x-test-backend", "x-test-spring"))
                .build();

        String res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
        createdPostIds.add(postId);

        List<String> tagSlugs = JsonPath.read(res, "$.tags[*].slug");
        assert tagSlugs.contains("x-test-backend");
        assert tagSlugs.contains("x-test-spring");
    }

    // 存在しないタグを指定して投稿作成失敗
    @Test
    void createPost_withUnknownTag_shouldReturn400() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");
        var editor = userRepository.findByEmail("editor@example.com").orElseThrow();

        var req = PostRequestDto.builder()
                .title("Invalid Tag Post")
                .slug("invalid-tag-post")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"body\"}]}")
                .authorId(editor.getId())
                .tags(List.of("unknown"))
                .build();

        mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // 投稿作成成功
    @Test
    void createPost_success_should_return_201() throws Exception {
        String accessToken = getAccessToken("author@example.com", "password123");
        // 実際のauthorIdを取得
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();
        var req = PostRequestDto.builder()
                .title("タイトル")
                .slug("test-title")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(authorId)
                .excerpt("概要")
                .build();
        var res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("タイトル"))
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
        createdPostIds.add(postId);
    }

    // タイトル空
    @Test
    void createPost_blankTitle_should_return_400() throws Exception {
        String accessToken = getAccessToken("author@example.com", "password123");

        // 実際のauthorIdを取得
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();

        var req = PostRequestDto.builder()
                .title("")
                .slug("test-title-2")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(authorId)
                .build();
        mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].field").value("title"));
    }

    // 他人による更新禁止
    @Test
    void updatePost_byOtherUser_should_return_403() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");
        String otherToken = getAccessToken("author@example.com", "password123");

        // 実際のauthorIdを取得
        Long authorId = userRepository.findByEmail("editor@example.com").orElseThrow().getId();

        String uniqueSlug = "original-slug-" + System.currentTimeMillis();

        // まず editor で投稿作成
        var req = PostRequestDto.builder()
                .title("original")
                .slug(uniqueSlug)
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"body\"}]}")
                .authorId(authorId)
                .build();
        var res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
        createdPostIds.add(postId);
        // other で更新
        var updateReq = PostRequestDto.builder()
                .title("hacked")
                .slug(uniqueSlug)
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"body\"}]}")
                .authorId(2L)
                .build();
        mockMvc.perform(put("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    // 存在しない投稿取得
    @Test
    void getPost_notFound_should_return_404() throws Exception {
        String accessToken = getAccessToken("author@example.com", "password123");
        mockMvc.perform(get("/api/admin/posts/999999")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // 未認証ユーザーによる作成は401
    @Test
    void createPost_unauthenticated_should_return_401() throws Exception {
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();
        var req = PostRequestDto.builder()
                .title("タイトル")
                .slug("unauth-title")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(authorId)
                .excerpt("概要")
                .build();
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // 他人の投稿削除は403
    @Test
    void deletePost_byOtherUser_should_return_403() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");
        String otherToken = getAccessToken("author@example.com", "password123");
        Long authorId = userRepository.findByEmail("editor@example.com").orElseThrow().getId();

        // 投稿作成
        var req = PostRequestDto.builder()
                .title("original")
                .slug("original-slug2")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"body\"}]}")
                .authorId(authorId)
                .excerpt("概要")
                .build();
        var res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
        createdPostIds.add(postId);

        // 他人で削除
        mockMvc.perform(delete("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // authorロールは自分の投稿だけ取得できる
    @Test
    void adminApi_access_by_author_should_return_own_posts_only() throws Exception {
        // authorユーザーで投稿を作成
        String authorToken = getAccessToken("author@example.com", "password123");
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();

        var req = PostRequestDto.builder()
                .title("author投稿")
                .slug("author-post-check-own")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(authorId)
                .excerpt("概要")
                .build();
        var res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
        createdPostIds.add(postId);

        // authorで一覧取得→全件のauthor.idが自分自身であることをassert
        var listRes = mockMvc.perform(get("/api/admin/posts")
                .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> authorIds = JsonPath.read(listRes, "$.content[*].author.id");
        for (Integer id : authorIds) {
            assert id.longValue() == authorId : "author以外の投稿が混入: id=" + id;
        }
        // 1件以上返ることも確認
        assert authorIds.size() > 0 : "authorの投稿が1件も返らない";
    }

    // authorはadmin/editorの投稿取得は403になることをIDごとに検証
    @Test
    void getPost_byAuthor_for_admin_and_editor_posts_should_return_403() throws Exception {
        String authorToken = getAccessToken("author@example.com", "password123");
        String adminToken = getAccessToken("admin@example.com", "password123");
        String editorToken = getAccessToken("editor@example.com", "password123");

        // まずadminで投稿作成
        Long adminId = userRepository.findByEmail("admin@example.com").orElseThrow().getId();
        var adminReq = PostRequestDto.builder()
                .title("admin投稿")
                .slug("admin-post-for-auth-test")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"admin本文\"}]}")
                .authorId(adminId)
                .excerpt("admin概要")
                .build();
        var adminRes = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long adminPostId = Long.valueOf((Integer) JsonPath.read(adminRes, "$.id"));
        createdPostIds.add(adminPostId);

        // editorで投稿作成
        Long editorId = userRepository.findByEmail("editor@example.com").orElseThrow().getId();
        var editorReq = PostRequestDto.builder()
                .title("editor投稿")
                .slug("editor-post-for-auth-test")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"editor本文\"}]}")
                .authorId(editorId)
                .excerpt("editor概要")
                .build();
        var editorRes = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(editorReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long editorPostId = Long.valueOf((Integer) JsonPath.read(editorRes, "$.id"));
        createdPostIds.add(editorPostId);

        // authorがadminの投稿を取得→403
        mockMvc.perform(get("/api/admin/posts/" + adminPostId)
                .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isForbidden());

        // authorがeditorの投稿を取得→403
        mockMvc.perform(get("/api/admin/posts/" + editorPostId)
                .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isForbidden());
    }

    // adminは全投稿を取得できる
    @Test
    void getPosts_should_return_all_posts_count() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");

        // DBから全件数を取得
        long dbCount = postRepository.count();

        // APIで全件取得（ページサイズを十分大きくする）
        mockMvc.perform(get("/api/admin/posts")
                .param("size", String.valueOf(dbCount > 0 ? dbCount : 100))
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value((int) dbCount));
    }

    // Editorでも全投稿取得できる
    @Test
    void getPosts_byEditor_should_return_all_posts_count() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");

        // DBから全件数を取得
        long dbCount = postRepository.count();

        // APIで全件取得（ページサイズを十分大きくする）
        mockMvc.perform(get("/api/admin/posts")
                .param("size", String.valueOf(dbCount > 0 ? dbCount : 100))
                .header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value((int) dbCount));
    }

    @Test
    void searchPosts_byTagFilter_should_return_matching_posts() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        Long adminId = userRepository.findByEmail("admin@example.com").orElseThrow().getId();

        Tag spring = tagRepository.save(Tag.builder().name("x-test-spring").slug("x-test-spring").build());
        Tag security = tagRepository.save(Tag.builder().name("x-test-security").slug("x-test-security").build());
        createdTagIds.add(spring.getId());
        createdTagIds.add(security.getId());

        var postSpring = PostRequestDto.builder()
                .title("Spring Tips")
                .slug("spring-tips")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"spring\"}]}")
                .authorId(adminId)
                .tags(List.of("x-test-spring"))
                .build();
        var postSecurity = PostRequestDto.builder()
                .title("Security Guide")
                .slug("security-guide")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"security\"}]}")
                .authorId(adminId)
                .tags(List.of("x-test-security"))
                .build();
        var postBoth = PostRequestDto.builder()
                .title("Spring Security")
                .slug("spring-security")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"spring security\"}]}")
                .authorId(adminId)
                .tags(List.of("x-test-spring", "x-test-security"))
                .build();

        String resSpring = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postSpring)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postSpringId = Long.valueOf((Integer) JsonPath.read(resSpring, "$.id"));
        createdPostIds.add(postSpringId);

        String resSecurity = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postSecurity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postSecurityId = Long.valueOf((Integer) JsonPath.read(resSecurity, "$.id"));
        createdPostIds.add(postSecurityId);

        String resBoth = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postBoth)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postBothId = Long.valueOf((Integer) JsonPath.read(resBoth, "$.id"));
        createdPostIds.add(postBothId);

        String listBySpring = mockMvc.perform(get("/api/admin/posts")
                .param("tag", "x-test-spring")
                .param("size", "10")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> springSlugs = JsonPath.read(listBySpring, "$.content[*].slug");
        assert springSlugs.contains("spring-tips");
        assert springSlugs.contains("spring-security");
        assert !springSlugs.contains("security-guide");

        String listByBoth = mockMvc.perform(get("/api/admin/posts")
                .param("tag", "x-test-spring,x-test-security")
                .param("size", "10")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> bothSlugs = JsonPath.read(listByBoth, "$.content[*].slug");
        assert bothSlugs.contains("spring-tips");
        assert bothSlugs.contains("spring-security");
        assert bothSlugs.contains("security-guide");
    }

    // slug重複
    @Test
    void createPost_duplicateSlug_should_return_409() throws Exception {
        String token = getAccessToken("author@example.com", "password123");
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();
        var req = PostRequestDto.builder()
                .title("タイトル1")
                .slug("dup-slug")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(authorId)
                .excerpt("概要")
                .build();
        // 1回目は成功
        var res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
        createdPostIds.add(postId);
        // 2回目は409
        mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ページングとソート
    @Test
    void getPosts_paging_and_sorting_should_return_correct_results() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();

        // 1. 既存の投稿数を取得
        long initialCount = postRepository.count();

        // 2. テストで投稿を3件作成
        int created = 3;
        for (int i = 1; i <= created; i++) {
            var req = PostRequestDto.builder()
                    .title("タイトル" + i)
                    .slug("paging-slug-" + i)
                    .status("DRAFT")
                    .contentJson("{\"ops\":[{\"insert\":\"本文" + i + "\"}]}")
                    .authorId(authorId)
                    .excerpt("概要" + i)
                    .build();
            var res = mockMvc.perform(post("/api/admin/posts")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));
            createdPostIds.add(postId);
        }

        // 3. 総件数
        long total = initialCount + created;
        int pageSize = 2;
        int totalPages = (int) Math.ceil((double) total / pageSize);

        // 4. 各ページの期待件数を計算
        for (int page = 0; page < totalPages; page++) {
            int expectedCount = (int) Math.min(pageSize, total - page * pageSize);
            mockMvc.perform(get("/api/admin/posts")
                    .param("page", String.valueOf(page))
                    .param("size", String.valueOf(pageSize))
                    .param("sort", "createdAt,desc")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(expectedCount));
        }
    }

    // 投稿者自身による削除（正常系）
    @Test
    void deletePost_byOwner_should_return_204_and_not_found_after() throws Exception {
        String authorToken = getAccessToken("author@example.com", "password123");
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();

        // 投稿作成
        var req = PostRequestDto.builder()
                .title("削除テスト")
                .slug("delete-slug")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"削除本文\"}]}")
                .authorId(authorId)
                .excerpt("概要")
                .build();
        var res = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(res, "$.id"));

        // 削除
        mockMvc.perform(delete("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNoContent());

        // 削除後の取得は404
        mockMvc.perform(get("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNotFound());
    }

    // 存在しない投稿の削除は404
    @Test
    void deletePost_notFound_should_return_404() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        mockMvc.perform(delete("/api/admin/posts/999999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // 投稿者自身による更新（正常系）
    @Test
    void updatePost_byOwner_should_return_200_and_reflect_changes() throws Exception {
        String authorToken = getAccessToken("author@example.com", "password123");
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();

        // 1. 投稿作成
        var createReq = PostRequestDto.builder()
                .title("更新前タイトル")
                .slug("update-slug")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"更新前本文\"}]}")
                .authorId(authorId)
                .excerpt("更新前概要")
                .build();
        var createRes = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdPostIds.add(postId);

        // 2. 更新リクエスト
        var updateReq = PostRequestDto.builder()
                .title("更新後タイトル")
                .slug("update-slug") // slugは変えずに更新
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"更新後本文\"}]}")
                .authorId(authorId)
                .excerpt("更新後概要")
                .build();

        mockMvc.perform(put("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value("更新後タイトル"))
                .andExpect(jsonPath("$.excerpt").value("更新後概要"))
                .andReturn().getResponse().getContentAsString();

        // 3. DBの値も反映されているか確認
        var updatedPost = postRepository.findById(postId).orElseThrow();
        assert updatedPost.getTitle().equals("更新後タイトル");
        assert updatedPost.getExcerpt().equals("更新後概要");
    }

    // ユーティリティ: ログインしてトークン取得
    String getAccessToken(String email, String password) throws Exception {
        var loginReq = new LoginRequestDto(email, password);
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(loginRes, "$.accessToken");
    }

    // author が PUBLISHED に更新しようとすると 403
    @Test
    void updatePost_statusToPublished_byAuthor_should_return_403() throws Exception {
        String authorToken = getAccessToken("author@example.com", "password123");
        Long authorId = userRepository.findByEmail("author@example.com").orElseThrow().getId();

        // 1. DRAFT 状態で投稿作成
        var createReq = PostRequestDto.builder()
                .title("公開権限テスト")
                .slug("publish-slug")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(authorId)
                .excerpt("概要")
                .build();
        var createRes = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdPostIds.add(postId);

        // 2. author が PUBLISHED にしようとする → 403
        var updateReq = PostRequestDto.builder()
                .title("公開権限テスト")
                .slug("publish-slug")
                .status("PUBLISHED")
                .contentJson("{\"ops\":[{\"insert\":\"本文更新\"}]}")
                .authorId(authorId)
                .excerpt("概要更新")
                .build();

        mockMvc.perform(put("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    // editor が DRAFT → PUBLISHED に更新できる（正常系）
    @Test
    void updatePost_statusToPublished_byEditor_should_return_200_and_set_publishedAt() throws Exception {
        String editorToken = getAccessToken("editor@example.com", "password123");
        Long editorId = userRepository.findByEmail("editor@example.com").orElseThrow().getId();

        // 1. DRAFT 状態で投稿作成
        var createReq = PostRequestDto.builder()
                .title("公開テスト")
                .slug("publish-slug-editor")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(editorId)
                .excerpt("概要")
                .build();
        var createRes = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdPostIds.add(postId);

        // 2. PUBLISHED に更新
        var updateReq = PostRequestDto.builder()
                .title("公開テスト更新")
                .slug("publish-slug-editor")
                .status("PUBLISHED")
                .contentJson("{\"ops\":[{\"insert\":\"公開本文\"}]}")
                .authorId(editorId)
                .excerpt("公開概要")
                .build();

        mockMvc.perform(put("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andReturn().getResponse().getContentAsString();

        // 3. DBの値も反映されているか確認
        var updatedPost = postRepository.findById(postId).orElseThrow();
        assert updatedPost.getStatus() == Post.Status.PUBLISHED;
        assert updatedPost.getPublishedAt() != null;
    }

    // admin が DRAFT → PUBLISHED に更新できる（正常系）
    @Test
    void updatePost_statusToPublished_byAdmin_should_return_200_and_set_publishedAt() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        Long adminId = userRepository.findByEmail("admin@example.com").orElseThrow().getId();
        // 1. DRAFT 状態で投稿作成
        var createReq = PostRequestDto.builder()
                .title("公開テストadmin")
                .slug("publish-slug-admin")
                .status("DRAFT")
                .contentJson("{\"ops\":[{\"insert\":\"本文\"}]}")
                .authorId(adminId)
                .excerpt("概要")
                .build();
        var createRes = mockMvc.perform(post("/api/admin/posts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf((Integer) JsonPath.read(createRes, "$.id"));
        createdPostIds.add(postId);

        // 2. PUBLISHED に更新
        var updateReq = PostRequestDto.builder()
                .title("公開テスト更新admin")
                .slug("publish-slug-admin")
                .status("PUBLISHED")
                .contentJson("{\"ops\":[{\"insert\":\"公開本文\"}]}")
                .authorId(adminId)
                .excerpt("公開概要")
                .build();
        mockMvc.perform(put("/api/admin/posts/" + postId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andReturn().getResponse().getContentAsString();

        // 3. DBの値も反映されているか確認
        var updatedPost = postRepository.findById(postId).orElseThrow();
        assert updatedPost.getStatus() == Post.Status.PUBLISHED;
        assert updatedPost.getPublishedAt() != null;
    }
}
