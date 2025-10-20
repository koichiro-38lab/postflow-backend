package com.example.backend.controller.pub;

import com.example.backend.entity.Category;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;
    private Category category1;

    @BeforeEach
    void setUp() {
        // テストデータのクリーンアップ
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        postRepository.flush();
        categoryRepository.flush();

        // 著者の作成
        author = userRepository.findAll().stream().findFirst().orElseThrow();

        // カテゴリの作成
        category1 = categoryRepository.save(Category.builder()
                .name("Technology")
                .slug("technology")
                .sortOrder(1)
                .build());
    }

    @Test
    void getCategories_returnsPublicCategories() throws Exception {
        // 公開投稿の作成（カテゴリが公開される条件）
        postRepository.save(Post.builder()
                .title("Published Post")
                .slug("published-post")
                .status(Post.Status.PUBLISHED)
                .excerpt("This is a published post")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .author(author)
                .category(category1)
                .publishedAt(java.time.LocalDateTime.now().minusDays(1))
                .build());

        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Technology"))
                .andExpect(jsonPath("$[0].slug").value("technology"));
    }

    @Test
    void getCategories_returnsEmptyListWhenNoPublicPosts() throws Exception {
        // 公開投稿がない場合
        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}