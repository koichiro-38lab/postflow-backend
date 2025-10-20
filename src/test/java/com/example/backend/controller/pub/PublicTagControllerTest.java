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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;
    private Category category;
    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        // テストデータのクリーンアップ
        postRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();
        postRepository.flush();
        tagRepository.flush();
        categoryRepository.flush();

        // 著者の作成
        author = userRepository.findAll().stream().findFirst().orElseThrow();

        // カテゴリの作成
        category = categoryRepository.save(Category.builder()
                .name("Technology")
                .slug("technology")
                .sortOrder(1)
                .build());

        // タグの作成
        tag1 = tagRepository.save(Tag.builder()
                .name("Java")
                .slug("java")
                .build());

        tag2 = tagRepository.save(Tag.builder()
                .name("Spring")
                .slug("spring")
                .build());
    }

    @Test
    void getTags_returnsPublicTags() throws Exception {
        // 公開投稿の作成（タグが公開される条件）
        Post post = Post.builder()
                .title("Published Post")
                .slug("published-post")
                .status(Post.Status.PUBLISHED)
                .excerpt("This is a published post")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .category(category)
                .publishedAt(java.time.LocalDateTime.now().minusDays(1))
                .build();
        post.setTags(List.of(tag1, tag2));
        postRepository.save(post);

        mockMvc.perform(get("/api/public/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].slug").exists());
    }

    @Test
    void getTags_returnsEmptyListWhenNoPublicPosts() throws Exception {
        // 公開投稿がない場合
        mockMvc.perform(get("/api/public/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}