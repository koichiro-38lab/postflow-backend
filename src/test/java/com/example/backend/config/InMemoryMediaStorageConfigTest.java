package com.example.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.backend.service.media.MediaStorage;
import com.example.backend.service.media.MediaStorage.ObjectNotFoundException;
import com.example.backend.service.media.MediaStorage.PresignedDownload;
import com.example.backend.service.media.MediaStorage.PresignedUpload;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMediaStorageConfigTest {

    private InMemoryMediaStorageConfig config;
    private MediaStorage mediaStorage;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        config = new InMemoryMediaStorageConfig();
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T10:00:00Z"), ZoneOffset.UTC);
        mediaStorage = config.inMemoryMediaStorage(fixedClock);
    }

    // PresignedUploadを返すことを確認
    @Test
    void createUploadUrl_should_return_presigned_upload() {
        // 前提
        String storageKey = "test-key";
        String contentType = "image/jpeg";
        long contentLength = 1024L;
        Duration ttl = Duration.ofHours(1);

        // 実行
        PresignedUpload result = mediaStorage.createUploadUrl(storageKey, contentType, contentLength, ttl);

        // 検証
        assertNotNull(result);
        assertEquals("http://localhost/mock-upload/" + storageKey, result.url());
        assertEquals(contentType, result.headers().get("Content-Type"));
        assertEquals(Instant.parse("2023-01-01T11:00:00Z"), result.expiresAt());
        assertEquals(storageKey, result.storageKey());

        // 既存キーに追加されていることを検証
        @SuppressWarnings("unchecked")
        Set<String> existingKeys = (Set<String>) ReflectionTestUtils.getField(mediaStorage, "existingKeys");
        assertNotNull(existingKeys, "Existing keys set should not be null");
        assertTrue(existingKeys.contains(storageKey));
    }

    // ダウンロードURLを返すことを確認
    @Test
    void createDownloadUrl_should_return_presigned_download_when_key_exists() {
        // 前提
        String storageKey = "existing-key";
        Duration ttl = Duration.ofMinutes(30);

        // まずアップロードURLを作成して既存キーに追加する
        mediaStorage.createUploadUrl(storageKey, "image/png", 512L, Duration.ofHours(1));

        // 実行
        PresignedDownload result = mediaStorage.createDownloadUrl(storageKey, ttl);

        // 検証
        assertNotNull(result);
        assertEquals("http://localhost/mock-download/" + storageKey, result.url());
        assertEquals(Instant.parse("2023-01-01T10:30:00Z"), result.expiresAt());
    }

    // ダウンロードURL作成時にキーが存在しない場合、ObjectNotFoundExceptionがスローされることを確認
    @Test
    void createDownloadUrl_should_throw_exception_when_key_not_exists() {
        // 前提
        String nonExistentKey = "non-existent-key";
        Duration ttl = Duration.ofMinutes(30);

        // 実行と検証
        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> mediaStorage.createDownloadUrl(nonExistentKey, ttl));

        assertTrue(exception.getMessage().contains(nonExistentKey));
    }

    // ensureObjectExistsがキー存在時に例外を投げないことを確認
    @Test
    void ensureObjectExists_should_not_throw_when_key_exists() {
        // 前提
        String storageKey = "existing-key";

        // まずアップロードURLを作成して既存キーに追加する
        mediaStorage.createUploadUrl(storageKey, "text/plain", 256L, Duration.ofHours(1));

        // 実行と検証 - 例外が投げられないこと
        assertDoesNotThrow(() -> mediaStorage.ensureObjectExists(storageKey));
    }

    // ensureObjectExistsがキー非存在時にObjectNotFoundExceptionを投げることを確認
    @Test
    void ensureObjectExists_should_throw_exception_when_key_not_exists() {
        // 前提
        String nonExistentKey = "non-existent-key";

        // 実行と検証
        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> mediaStorage.ensureObjectExists(nonExistentKey));

        assertTrue(exception.getMessage().contains(nonExistentKey));
    }

    // deleteObjectがキー存在時に例外を投げないことを確認
    @Test
    void deleteObject_should_remove_key_from_existing_keys() {
        // 前提
        String storageKey = "key-to-delete";

        // まずアップロードURLを作成して既存キーに追加する
        mediaStorage.createUploadUrl(storageKey, "application/json", 128L, Duration.ofHours(1));

        // 削除前にキーが存在することを確認
        @SuppressWarnings("unchecked")
        Set<String> existingKeys = (Set<String>) ReflectionTestUtils.getField(mediaStorage, "existingKeys");
        assertNotNull(existingKeys, "Existing keys set should not be null");
        assertTrue(existingKeys.contains(storageKey));

        // 実行
        mediaStorage.deleteObject(storageKey);

        // 検証
        assertFalse(existingKeys.contains(storageKey));

        // 削除後にダウンロードを試みると例外が投げられることを検証
        assertThrows(ObjectNotFoundException.class,
                () -> mediaStorage.createDownloadUrl(storageKey, Duration.ofMinutes(10)));
    }

    // deleteObjectがキー非存在時に例外を投げないことを確認
    @Test
    void deleteObject_should_not_throw_when_key_not_exists() {
        // 前提
        String nonExistentKey = "non-existent-key";

        // 実行と検証 - 例外が投げられないこと
        assertDoesNotThrow(() -> mediaStorage.deleteObject(nonExistentKey));
    }
}