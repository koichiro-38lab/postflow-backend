package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.config.FakeMediaStorageConfig;
import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.media.MediaCreateRequestDto;
import com.example.backend.dto.media.MediaPresignRequestDto;
import com.example.backend.dto.media.MediaPresignResponseDto;
import com.example.backend.dto.media.MediaResponseDto;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.repository.MediaRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;

@SpringBootTest
@Import({ TestDataConfig.class, TestClockConfig.class, FakeMediaStorageConfig.class })
@ActiveProfiles("test")
@Transactional
class MediaServiceTest {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private PostRepository postRepository;

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        postRepository.flush();
        mediaRepository.deleteAll();
        mediaRepository.flush();
    }

    // アップロード用の事前署名URL取得テスト
    @Test
    void requestUpload_withValidData_shouldReturnPresignResponse() {
        // ユーザー取得
        User author = userRepository.findByEmail("author@example.com").orElseThrow();
        MediaPresignRequestDto dto = MediaPresignRequestDto.builder()
                .filename("test-image.jpg")
                .mime("image/jpeg")
                .bytes(1024L)
                .build();

        // アップロード用の事前署名URL取得
        MediaPresignResponseDto response = mediaService.requestUpload(dto, author);

        // 検証
        assertThat(response).isNotNull();
        assertThat(response.getUploadUrl()).isNotNull();
        assertThat(response.getStorageKey()).isNotNull();
        assertThat(response.getStorageKey()).endsWith(".jpg");
    }

    // メディア登録テスト
    @Test
    void register_withValidData_shouldCreateMedia() {
        // ユーザー
        User author = userRepository.findByEmail("author@example.com").orElseThrow();
        String storageKey = "uploads/test-image.jpg";

        // シミュレートアップロード先にファイルを配置
        var mediaStorageField = org.springframework.test.util.ReflectionTestUtils.getField(mediaService,
                "mediaStorage");
        assertThat(mediaStorageField).isNotNull();
        if (mediaStorageField instanceof FakeMediaStorageConfig.InMemoryMediaStorage) {
            ((FakeMediaStorageConfig.InMemoryMediaStorage) mediaStorageField)
                    .simulateUpload(storageKey);
        }

        // メディア登録DTO
        MediaCreateRequestDto dto = MediaCreateRequestDto.builder()
                .filename("test-image.jpg")
                .storageKey(storageKey)
                .mime("image/jpeg")
                .bytes(1024L)
                .build();

        // 登録実行
        MediaResponseDto response = mediaService.register(dto, author);

        // 検証
        assertThat(response).isNotNull();
        assertThat(response.getFilename()).isEqualTo("test-image.jpg");
        assertThat(response.getMime()).isEqualTo("image/jpeg");
        assertThat(response.getBytes()).isEqualTo(1024L);
        assertThat(response.getStorageKey()).isEqualTo(storageKey);
        assertThat(response.getCreatedBy().getId()).isEqualTo(author.getId());
    }

    // メディア一覧取得テスト
    @Test
    void list_withAuthor_shouldReturnOnlyOwnMedia() {
        // ユーザー取得
        User author = userRepository.findByEmail("author@example.com").orElseThrow();
        User editor = userRepository.findByEmail("editor@example.com").orElseThrow();

        // authorのメディアの初期数を取得
        Page<MediaResponseDto> initialAuthorMedia = mediaService.list(null, null, Pageable.unpaged(), author);
        int initialCount = initialAuthorMedia.getContent().size();

        String authorStorageKey = "uploads/test-author-image.jpg";
        String editorStorageKey = "uploads/test-editor-image.jpg";

        // シミュレートアップロード先にファイルを配置
        var mediaStorage = (com.example.backend.config.FakeMediaStorageConfig.InMemoryMediaStorage) org.springframework.test.util.ReflectionTestUtils
                .getField(mediaService, "mediaStorage");
        if (mediaStorage != null) {
            mediaStorage.simulateUpload(authorStorageKey);
            mediaStorage.simulateUpload(editorStorageKey);
        }

        // authorのメディア作成
        MediaCreateRequestDto dto1 = MediaCreateRequestDto.builder()
                .filename("test-author-image.jpg")
                .storageKey(authorStorageKey)
                .mime("image/jpeg")
                .bytes(1024L)
                .build();
        mediaService.register(dto1, author);

        // editorのメディア作成
        MediaCreateRequestDto dto2 = MediaCreateRequestDto.builder()
                .filename("test-editor-image.jpg")
                .storageKey(editorStorageKey)
                .mime("image/jpeg")
                .bytes(2048L)
                .build();
        mediaService.register(dto2, editor);

        // authorがメディア一覧を取得
        Page<MediaResponseDto> finalAuthorMedia = mediaService.list(null, null, Pageable.unpaged(), author);

        // 検証 authorのメディアのみが含まれること
        assertThat(finalAuthorMedia.getContent().size()).isEqualTo(initialCount + 1);
        assertThat(finalAuthorMedia.getContent())
                .anyMatch(media -> media.getFilename().equals("test-author-image.jpg") &&
                        media.getCreatedBy().getId().equals(author.getId()));
    }

    // メディア削除の権限チェックテスト
    @Test
    void delete_withInsufficientPermissions_shouldThrowAccessDeniedException() {
        // ユーザー取得
        User author1 = userRepository.findByEmail("author@example.com").orElseThrow();
        User author2 = User.builder()
                .email("author2@example.com")
                .passwordHash("hashed")
                .displayName("Author 2")
                .role(User.Role.AUTHOR)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(author2);

        String storageKey = "uploads/author-media.jpg";

        // シミュレーションアップロード先にファイルを配置
        var mediaStorageField = org.springframework.test.util.ReflectionTestUtils.getField(mediaService,
                "mediaStorage");
        assertThat(mediaStorageField).isNotNull();
        if (mediaStorageField instanceof FakeMediaStorageConfig.InMemoryMediaStorage) {
            ((FakeMediaStorageConfig.InMemoryMediaStorage) mediaStorageField)
                    .simulateUpload(storageKey);
        }

        // author1がメディアを作成
        MediaCreateRequestDto createDto = MediaCreateRequestDto.builder()
                .filename("author-media.jpg")
                .storageKey(storageKey)
                .mime("image/jpeg")
                .bytes(1024L)
                .build();
        MediaResponseDto createdMedia = mediaService.register(createDto, author1);

        // author2がauthor1のメディアを削除しようとすると権限エラーになることを検証
        assertThatThrownBy(() -> mediaService.delete(createdMedia.getId(), author2))
                .isInstanceOf(com.example.backend.exception.AccessDeniedException.class)
                .hasMessage("You do not have permission to access this media");
    }
}