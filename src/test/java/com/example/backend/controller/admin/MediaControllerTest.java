package com.example.backend.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.example.backend.config.FakeMediaStorageConfig;
import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.auth.LoginRequestDto;
import com.example.backend.dto.media.MediaCreateRequestDto;
import com.example.backend.dto.media.MediaPresignRequestDto;
import com.example.backend.dto.media.MediaPresignResponseDto;
import com.example.backend.dto.media.MediaResponseDto;
import com.example.backend.entity.Post;
import com.example.backend.repository.MediaRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestClockConfig.class, TestDataConfig.class, FakeMediaStorageConfig.class })
@org.springframework.test.context.ActiveProfiles("test")
class MediaControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MediaRepository mediaRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    FakeMediaStorageConfig.InMemoryMediaStorage mediaStorage;

    private final List<Long> createdMediaIds = new ArrayList<>();
    private final List<Long> createdPostIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        // 外部キー制約を避けるため、投稿を先に削除
        createdPostIds.forEach(postRepository::deleteById);
        createdPostIds.clear();

        // 作成したメディアのみ削除
        createdMediaIds.forEach(mediaRepository::deleteById);
        createdMediaIds.clear();
    }

    // presignエンドポイントでアップロード用storageKeyとURLが返ることを検証
    @Test
    void presignUpload_shouldReturnStorageKey() throws Exception {
        String token = getAccessToken("admin@example.com", "password123");
        MediaPresignRequestDto req = MediaPresignRequestDto.builder()
                .filename("sample.png")
                .mime("image/png")
                .bytes(1024L)
                .width(200)
                .height(100)
                .build();

        String response = mockMvc.perform(post("/api/admin/media/presign")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageKey").exists())
                .andReturn().getResponse().getContentAsString();

        MediaPresignResponseDto dto = objectMapper.readValue(response, MediaPresignResponseDto.class);
        assertThat(dto.getStorageKey()).isNotBlank();
        assertThat(dto.getUploadUrl()).isNotBlank();
    }

    // presign後にアップロードし、登録APIを呼ぶとメディアが登録されることを検証
    @Test
    void registerMedia_success() throws Exception {
        String token = getAccessToken("admin@example.com", "password123");
        MediaResponseDto media = createMedia(token, "hero.jpg", "image/jpeg");
        assertThat(media.getId()).isNotNull();
        assertThat(media.getStorageKey()).isNotBlank();
    }

    // presign後にアップロードせず登録APIを呼ぶと400エラーとなることを検証
    @Test
    void registerMedia_withoutUploadedObject_shouldReturn400() throws Exception {
        String token = getAccessToken("admin@example.com", "password123");
        MediaPresignResponseDto presign = requestPresign(token, "ghost.png", "image/png");

        MediaCreateRequestDto createReq = MediaCreateRequestDto.builder()
                .filename("ghost.png")
                .mime("image/png")
                .bytes(5000L)
                .storageKey(presign.getStorageKey())
                .build();

        mockMvc.perform(post("/api/admin/media")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isBadRequest());
    }

    // AUTHOR権限ユーザーは自分が登録したメディアのみ取得できることを検証
    @Test
    void listMedia_authorShouldSeeOnlyOwnItems() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        String authorToken = getAccessToken("author@example.com", "password123");
        var author = userRepository.findByEmail("author@example.com").orElseThrow();

        createMedia(adminToken, "admin-asset.png", "image/png");
        MediaResponseDto authorMedia = createMedia(authorToken, "author-asset.png", "image/png");

        String response = mockMvc.perform(get("/api/admin/media")
                .header("Authorization", "Bearer " + authorToken)
                .param("size", "50")
                .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> creatorIds = JsonPath.read(response, "$.content[*].createdBy.id");
        List<String> filenames = JsonPath.read(response, "$.content[*].filename");
        assertThat(creatorIds).isNotEmpty();
        assertThat(creatorIds).allMatch(id -> id.longValue() == author.getId());
        assertThat(filenames).contains(authorMedia.getFilename());
        assertThat(filenames).doesNotContain("admin-asset.png");
    }

    // 投稿のカバー画像として利用中のメディアを削除しようとすると409エラーとなることを検証
    @Test
    void deleteMedia_inUseAsCover_shouldReturn409() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        MediaResponseDto media = createMedia(adminToken, "cover.png", "image/png");

        var admin = userRepository.findByEmail("admin@example.com").orElseThrow();
        Post post = Post.builder()
                .title("Media bound post")
                .slug("media-bound-post-" + media.getId())
                .status(Post.Status.DRAFT)
                .contentJson("{\"ops\":[]}")
                .author(admin)
                .coverMedia(mediaRepository.findById(media.getId()).orElseThrow())
                .build();
        Post saved = postRepository.save(post);
        createdPostIds.add(saved.getId());

        mockMvc.perform(delete("/api/admin/media/" + media.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    // 未使用のメディアは正常に削除できることを検証
    @Test
    void deleteMedia_success() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        MediaResponseDto media = createMedia(adminToken, "delete-me.png", "image/png");

        mockMvc.perform(delete("/api/admin/media/" + media.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ダウンロードAPIでpresigned URLと有効期限が返ることを検証
    @Test
    void downloadMedia_shouldReturnPresignedUrl() throws Exception {
        String adminToken = getAccessToken("admin@example.com", "password123");
        MediaResponseDto media = createMedia(adminToken, "download.png", "image/png");

        mockMvc.perform(get("/api/admin/media/" + media.getId() + "/download")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").exists())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    // メディア一覧取得（keywordフィルタ）
    @Test
    void listMedia_withKeywordFilter() throws Exception {
        String token = getAccessToken("admin@example.com", "password123");

        // テスト固有のメディアを作成（完全にユニークなキーワード）
        createMedia(token, "unique999hero777.png", "image/png");
        createMedia(token, "unique999hero888.jpg", "image/jpeg");
        createMedia(token, "differentnomatch123.jpg", "image/jpeg");

        String response = mockMvc.perform(get("/api/admin/media")
                .param("keyword", "unique999hero")
                .param("size", "10")
                .param("sort", "createdAt,desc")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // "unique999hero"を含むファイル名のみが返されることを確認
        List<String> filenames = JsonPath.read(response, "$.content[*].filename");
        assertThat(filenames).hasSize(2);
        assertThat(filenames).allMatch(name -> name.contains("unique999hero"));
    }

    // メディア一覧取得（mimeとkeyword両方フィルタ）
    @Test
    void listMedia_withMimeAndKeywordFilters() throws Exception {
        String token = getAccessToken("admin@example.com", "password123");

        // テスト固有のメディアを作成（完全にユニークなキーワード）
        createMedia(token, "ultra888unique666.png", "image/png");
        createMedia(token, "ultra888unique777.mp4", "video/mp4");
        createMedia(token, "otherdifferent999.jpg", "image/jpeg");

        String response = mockMvc.perform(get("/api/admin/media")
                .param("mime", "image")
                .param("keyword", "ultra888unique")
                .param("size", "10")
                .param("sort", "createdAt,desc")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // "ultra888unique"を含み、かつimage mimeタイプのファイルのみが返されることを確認
        List<String> filenames = JsonPath.read(response, "$.content[*].filename");
        List<String> mimeTypes = JsonPath.read(response, "$.content[*].mime");

        assertThat(filenames).hasSize(1);
        assertThat(filenames).allMatch(name -> name.contains("ultra888unique"));
        assertThat(mimeTypes).allMatch(mime -> mime.startsWith("image"));
        assertThat(filenames).contains("ultra888unique666.png");
    }

    // extractExtensionのさまざまなケースをカバーするためのpresignテスト
    @Test
    void presignUpload_variousFilenames() throws Exception {
        String token = getAccessToken("admin@example.com", "password123");

        // 拡張子なし
        requestPresign(token, "noextension", "application/octet-stream");

        // ドット始まり
        requestPresign(token, ".hidden", "application/octet-stream");

        // ドット終わり
        requestPresign(token, "nodot.", "application/octet-stream");

        // 正常な拡張子
        requestPresign(token, "normal.jpg", "image/jpeg");

        // 大文字拡張子
        requestPresign(token, "upper.PNG", "image/png");
    }

    // ログインしてJWTトークンを取得するヘルパー
    private String getAccessToken(String email, String password) throws Exception {
        var loginReq = new LoginRequestDto(email, password);
        var loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(loginRes, "$.accessToken");
    }

    // メディア作成のヘルパー
    private MediaResponseDto createMedia(String token, String filename, String mime) throws Exception {
        MediaPresignResponseDto presign = requestPresign(token, filename, mime);
        mediaStorage.simulateUpload(presign.getStorageKey());

        MediaCreateRequestDto createReq = MediaCreateRequestDto.builder()
                .filename(filename)
                .mime(mime)
                .bytes(12345L)
                .storageKey(presign.getStorageKey())
                .width(1200)
                .height(800)
                .altText("alt text")
                .build();

        String response = mockMvc.perform(post("/api/admin/media")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.storageKey").value(presign.getStorageKey()))
                .andReturn().getResponse().getContentAsString();

        MediaResponseDto dto = objectMapper.readValue(response, MediaResponseDto.class);
        createdMediaIds.add(dto.getId());
        return dto;
    }

    // presignエンドポイントを呼び出すヘルパー
    private MediaPresignResponseDto requestPresign(String token, String filename, String mime) throws Exception {
        MediaPresignRequestDto req = MediaPresignRequestDto.builder()
                .filename(filename)
                .mime(mime)
                .bytes(2000L)
                .width(600)
                .height(400)
                .build();

        String response = mockMvc.perform(post("/api/admin/media/presign")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, MediaPresignResponseDto.class);
    }
}
