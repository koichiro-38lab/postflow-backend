package com.example.backend.controller.pub;

import com.example.backend.entity.Category;
import com.example.backend.entity.Post;
import com.example.backend.entity.Tag;
import com.example.backend.entity.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.TagRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private Clock clock;

    private User author;
    private Category category;
    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        // テストデータのクリーンアップ（@Transactionalでロールバックされるので完全削除）
        postRepository.deleteAll();
        postRepository.flush(); // 即座に削除を反映

        // 著者の作成（既存のテストデータを使用）
        author = userRepository.findAll().stream().findFirst().orElseThrow();

        // カテゴリの作成（既存データがあれば取得、なければ作成）
        category = categoryRepository.findAll().stream()
                .filter(c -> "technology".equals(c.getSlug()))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Technology")
                        .slug("technology")
                        .sortOrder(1)
                        .build()));

        // タグの作成（既存データがあれば取得、なければ作成）
        tag1 = tagRepository.findBySlug("java")
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .name("Java")
                        .slug("java")
                        .build()));

        tag2 = tagRepository.findBySlug("spring")
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .name("Spring")
                        .slug("spring")
                        .build()));
    }

    // 各投稿の状態と公開日時に基づく表示テスト
    @Test
    void getPosts_returnsPublishedPosts() throws Exception {
        // 公開済み投稿の作成
        postRepository.save(Post.builder()
                .title("Published Post")
                .slug("published-post")
                .status(Post.Status.PUBLISHED)
                .excerpt("This is a published post")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .category(category)
                .tags(List.of(tag1, tag2))
                .publishedAt(LocalDateTime.now(clock).minusDays(1))
                .build());

        // 下書き投稿（表示されないはず）
        postRepository.save(Post.builder()
                .title("Draft Post")
                .slug("draft-post")
                .status(Post.Status.DRAFT)
                .excerpt("This is a draft post")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .category(category)
                .publishedAt(null)
                .build());

        // 未来の投稿（表示されないはず）
        postRepository.save(Post.builder()
                .title("Future Post")
                .slug("future-post")
                .status(Post.Status.PUBLISHED)
                .excerpt("This is a future post")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .category(category)
                .publishedAt(LocalDateTime.now(clock).plusDays(1))
                .build());

        mockMvc.perform(get("/api/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("published-post"))
                .andExpect(jsonPath("$.content[0].title").value("Published Post"))
                .andExpect(jsonPath("$.content[0].tags.length()").value(2));
    }

    // タグフィルターによる投稿の取得テスト
    @Test
    void getPosts_withTagFilter_returnsFilteredPosts() throws Exception {
        // Javaタグ付き投稿
        postRepository.save(Post.builder()
                .title("Java Post")
                .slug("java-post")
                .status(Post.Status.PUBLISHED)
                .excerpt("Java content")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .category(category)
                .tags(List.of(tag1))
                .publishedAt(LocalDateTime.now(clock).minusDays(1))
                .build());

        // Springタグ付き投稿
        postRepository.save(Post.builder()
                .title("Spring Post")
                .slug("spring-post")
                .status(Post.Status.PUBLISHED)
                .excerpt("Spring content")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .category(category)
                .tags(List.of(tag2))
                .publishedAt(LocalDateTime.now(clock).minusDays(1))
                .build());

        mockMvc.perform(get("/api/public/posts?tag=java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("java-post"));
    }

    // スラッグによる投稿詳細の取得テスト
    @Test
    void getPostBySlug_returnsPublishedPost() throws Exception {
        postRepository.save(Post.builder()
                .title("Test Post")
                .slug("test-post")
                .status(Post.Status.PUBLISHED)
                .excerpt("Test excerpt")
                .contentJson(
                        "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}]}")
                .author(author)
                .category(category)
                .tags(List.of(tag1))
                .publishedAt(LocalDateTime.now(clock).minusDays(1))
                .build());

        mockMvc.perform(get("/api/public/posts/test-post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-post"))
                .andExpect(jsonPath("$.title").value("Test Post"))
                .andExpect(jsonPath("$.contentJson").exists())
                .andExpect(jsonPath("$.author").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.tags").isArray());
    }

    // 下書き状態の投稿取得テスト
    @Test
    void getPostBySlug_draftPost_returns404() throws Exception {
        postRepository.save(Post.builder()
                .title("Draft Post")
                .slug("draft-post")
                .status(Post.Status.DRAFT)
                .excerpt("Draft excerpt")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .build());

        mockMvc.perform(get("/api/public/posts/draft-post"))
                .andExpect(status().isNotFound());
    }

    // 存在しないスラッグの投稿取得テスト
    @Test
    void getPostBySlug_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/public/posts/non-existent"))
                .andExpect(status().isNotFound());
    }

    // ページネーションの動作確認テスト
    @Test
    void getPosts_withPagination_returnsPaginatedResults() throws Exception {
        // 3つの公開投稿を作成
        for (int i = 1; i <= 3; i++) {
            postRepository.save(Post.builder()
                    .title("Post " + i)
                    .slug("post-" + i)
                    .status(Post.Status.PUBLISHED)
                    .excerpt("Excerpt " + i)
                    .contentJson("{\"type\":\"doc\",\"content\":[]}")
                    .author(author)
                    .category(category)
                    .publishedAt(LocalDateTime.now(clock).minusDays(i))
                    .build());
        }

        mockMvc.perform(get("/api/public/posts?size=2&page=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
