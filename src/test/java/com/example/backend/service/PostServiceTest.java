package com.example.backend.service;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.post.PostRequestDto;
import com.example.backend.dto.post.PostResponseDto;
import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({ TestDataConfig.class, TestClockConfig.class })
@ActiveProfiles("test")
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        postRepository.flush();
    }

    // 過去の publishedAt 日付で投稿作成するテスト
    @Test
    void createPost_withPastPublishedAt_shouldSetPublishedAtToNull() {
        // ユーザー取得
        User author = userRepository.findByEmail("author@example.com").orElseThrow();

        // 投稿作成DTO
        PostRequestDto dto = PostRequestDto.builder()
                .title("Test Post")
                .slug("test-slug-" + System.currentTimeMillis())
                .status("DRAFT")
                .contentJson("{\"ops\": [{\"insert\": \"Test content\"}]}")
                .authorId(author.getId())
                .publishedAt(OffsetDateTime.now().minusDays(1)) // Past date
                .build();

        // 投稿作成
        PostResponseDto result = postService.create(dto, author);

        // 結果の検証
        assertThat(result.getPublishedAt()).isNull();
    }

    // 未来の publishedAt 日付で投稿作成するテスト
    @Test
    void createPost_withFuturePublishedAt_shouldSetPublishedAt() {
        // ユーザー取得
        User author = userRepository.findByEmail("author@example.com").orElseThrow();

        // 投稿作成DTO
        OffsetDateTime future = OffsetDateTime.now().plusDays(1);
        PostRequestDto dto = PostRequestDto.builder()
                .title("Future Post")
                .slug("future-slug-" + System.currentTimeMillis())
                .status("DRAFT")
                .contentJson("{\"ops\": [{\"insert\": \"Future content\"}]}")
                .authorId(author.getId())
                .publishedAt(future)
                .build();

        // 投稿作成
        PostResponseDto result = postService.create(dto, author);

        // 結果の検証
        assertThat(result.getPublishedAt()).isEqualTo(future);
    }

    // 投稿更新で権限不足の場合にAccessDeniedExceptionがスローされるテスト
    @Test
    void updatePost_withInsufficientPermissions_shouldThrowAccessDeniedException() {
        // ユーザー取得
        User author = userRepository.findByEmail("author@example.com").orElseThrow();

        // 投稿を作成
        PostRequestDto createDto = PostRequestDto.builder()
                .title("Original Title")
                .slug("original-slug-" + System.currentTimeMillis())
                .status("DRAFT")
                .contentJson("{\"ops\": [{\"insert\": \"Original content\"}]}")
                .authorId(author.getId())
                .build();
        PostResponseDto createdPost = postService.create(createDto, author);

        // 投稿更新DTO
        PostRequestDto updateDto = PostRequestDto.builder()
                .title("Updated Title")
                .slug("updated-slug-" + System.currentTimeMillis())
                .status("PUBLISHED") // Authors cannot publish posts
                .contentJson("{\"ops\": [{\"insert\": \"Updated content\"}]}")
                .authorId(author.getId())
                .build();

        // 検証
        assertThatThrownBy(() -> postService.update(createdPost.getId(), updateDto, author))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authors cannot publish posts");
    }

    // authorId が null の場合に例外がスローされるテスト
    @Test
    void createPost_withNullAuthorId_shouldThrowException() {
        // ユーザー取得
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // 投稿作成DTO (authorId が null)
        PostRequestDto dto = PostRequestDto.builder()
                .title("Test Post")
                .slug("test-slug-" + System.currentTimeMillis())
                .status("DRAFT")
                .contentJson("{\"ops\": [{\"insert\": \"Test content\"}]}")
                .authorId(null) // null authorId
                .build();

        // 検証
        assertThatThrownBy(() -> postService.create(dto, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("authorId is required for post creation");
    }

    // 存在しない投稿を更新しようとした場合のテスト
    @Test
    void updatePost_withNonExistentId_shouldReturnEmpty() {
        // ユーザー取得
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // 存在しないID
        Long nonExistentId = 999999L;

        // 投稿更新DTO
        PostRequestDto updateDto = PostRequestDto.builder()
                .title("Updated Title")
                .slug("updated-slug")
                .status("DRAFT")
                .contentJson("{\"ops\": [{\"insert\": \"Updated content\"}]}")
                .build();

        // 検証
        assertThat(postService.update(nonExistentId, updateDto, admin)).isEmpty();
    }

    // 存在しない投稿にアクセスしようとした場合のテスト
    @Test
    void findById_withNonExistentId_shouldReturnEmpty() {
        // ユーザー取得
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // 存在しないID
        Long nonExistentId = 999999L;

        // 検証
        assertThat(postService.findByIdWithAccessControl(nonExistentId, admin)).isEmpty();
    }

    // 不正なステータス値で検索した場合のテスト
    @Test
    void searchPosts_withInvalidStatus_shouldIgnoreInvalidStatus() {
        // ユーザー取得
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // 不正なステータスで検索
        var result = postService.searchWithAccessControl(
                null, null, "INVALID_STATUS", null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10),
                admin);

        // 検証（例外がスローされず、結果が返ることを確認）
        assertThat(result).isNotNull();
    }

    // 空のタグパラメータで検索した場合のテスト
    @Test
    void searchPosts_withEmptyTagParam_shouldReturnResults() {
        // ユーザー取得
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // 空のタグパラメータで検索
        var result = postService.searchWithAccessControl(
                null, null, null, null, null, "",
                org.springframework.data.domain.PageRequest.of(0, 10),
                admin);

        // 検証
        assertThat(result).isNotNull();
    }

    // カンマ区切りの空白を含むタグパラメータで検索した場合のテスト
    @Test
    void searchPosts_withWhitespaceInTagParam_shouldTrimAndFilter() {
        // ユーザー取得
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // カンマ区切りの空白を含むタグパラメータで検索
        var result = postService.searchWithAccessControl(
                null, null, null, null, null, " , , ",
                org.springframework.data.domain.PageRequest.of(0, 10),
                admin);

        // 検証（例外がスローされず、結果が返ることを確認）
        assertThat(result).isNotNull();
    }
}