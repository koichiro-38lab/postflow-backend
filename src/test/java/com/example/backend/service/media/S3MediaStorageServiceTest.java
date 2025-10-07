package com.example.backend.service.media;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.example.backend.config.MediaStorageProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;

import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3MediaStorageServiceTest {

    @Mock
    S3Presigner presigner;

    @Mock
    S3Client client;

    @Mock
    MediaStorageProperties properties;

    Clock clock = Clock.fixed(Instant.parse("2025-10-06T00:00:00Z"), java.time.ZoneOffset.UTC);

    S3MediaStorageService service;

    @BeforeEach
    void setup() {
        org.mockito.Mockito.lenient().when(properties.getBucket()).thenReturn("test-bucket");
        org.mockito.Mockito.lenient().when(properties.getRegion()).thenReturn("us-east-1");
        service = new S3MediaStorageService(client, presigner, properties, clock);
    }

    // ストレージキーがnullの場合、IllegalArgumentExceptionがスローされることを確認
    @Test
    void createUploadUrl_validatesStorageKey() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createUploadUrl(null, "image/png", 10, Duration.ofMinutes(5)));
    }

    // コンテンツ長が0の場合、IllegalArgumentExceptionがスローされることを確認
    @Test
    void createUploadUrl_validatesContentLength() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createUploadUrl("k", "image/png", 0, Duration.ofMinutes(5)));
    }

    // ダウンロードURL作成時にストレージキーがnullの場合、IllegalArgumentExceptionがスローされることを確認
    @Test
    void createDownloadUrl_validatesStorageKey() {
        assertThrows(IllegalArgumentException.class, () -> service.createDownloadUrl(null, Duration.ofMinutes(5)));
    }

    // アップロードURL作成時にSdkClientExceptionが発生した場合、StorageExceptionにラップされることを確認
    @Test
    void createUploadUrl_presignerExceptionWrapped() {
        SdkClientException cause = SdkClientException.create("fail", null);
        when(presigner.presignPutObject(
                org.mockito.ArgumentMatchers.<software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest>any()))
                .thenThrow(cause);

        assertThrows(MediaStorage.StorageException.class,
                () -> service.createUploadUrl("key", "image/png", 10, Duration.ofMinutes(5)));
    }

    // ダウンロードURL作成時にSdkClientExceptionが発生した場合、StorageExceptionにラップされることを確認
    @Test
    void createDownloadUrl_presignerExceptionWrapped() {
        SdkClientException cause2 = SdkClientException.create("fail", null);
        when(presigner.presignGetObject(
                org.mockito.ArgumentMatchers.<software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest>any()))
                .thenThrow(cause2);

        assertThrows(MediaStorage.StorageException.class,
                () -> service.createDownloadUrl("key", Duration.ofMinutes(5)));
    }
}

class MediaStorageObjectNotFoundExceptionTest {

    // cause を指定したコンストラクタの動作検証（メッセージと cause の設定を確認）
    @Test
    void constructor_withCause_setsMessageAndCause() {
        Throwable cause = new IllegalArgumentException("underlying-cause");
        MediaStorage.ObjectNotFoundException ex = new MediaStorage.ObjectNotFoundException("my-key", cause);

        assertThat(ex.getMessage()).contains("my-key");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    // cause を指定しないコンストラクタの動作検証（メッセージの設定を確認、cause は null）
    @Test
    void constructor_withoutCause_setsMessage() {
        MediaStorage.ObjectNotFoundException ex = new MediaStorage.ObjectNotFoundException("simple-key");

        assertThat(ex.getMessage()).contains("simple-key");
        assertThat(ex.getCause()).isNull();
    }
}

@ExtendWith(MockitoExtension.class)
class S3MediaStorageServiceEnsureDeleteTest {

    @Mock
    S3Client client;

    @Mock
    S3MediaStorageService unusedPresigner;

    @Mock
    MediaStorageProperties properties;

    Clock clock = Clock.fixed(Instant.parse("2025-10-06T00:00:00Z"), java.time.ZoneOffset.UTC);

    S3MediaStorageService service;

    @BeforeEach
    void setup() {
        // 他のテストが properties に依存している場合に備えて lenient
        org.mockito.Mockito.lenient().when(properties.getBucket()).thenReturn("test-bucket");
        // create a service with a dummy presigner (not used in these tests)
        service = new S3MediaStorageService(client, null, properties, clock);
    }

    // オブジェクトが存在する場合、例外が発生しないことを確認
    @Test
    void ensureObjectExists_success_noException() {
        when(client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertDoesNotThrow(() -> service.ensureObjectExists("some-key"));
    }

    // オブジェクトが存在する場合、例外が発生しないことを確認
    @Test
    void ensureObjectExists_noSuchKey_wrappedAsObjectNotFound() {
        when(client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("no key").build());

        assertThrows(MediaStorage.ObjectNotFoundException.class, () -> service.ensureObjectExists("missing-key"));
    }

    // S3Exception 404が発生した場合、ObjectNotFoundExceptionにラップされることを確認
    @Test
    void ensureObjectExists_s3Exception404_wrappedAsObjectNotFound() {
        when(client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("not found").build());

        assertThrows(MediaStorage.ObjectNotFoundException.class, () -> service.ensureObjectExists("missing-key"));
    }

    // その他のS3Exceptionが発生した場合、StorageExceptionにラップされることを確認
    @Test
    void ensureObjectExists_s3ExceptionOther_wrappedAsStorageException() {
        when(client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("server error").build());

        assertThrows(MediaStorage.StorageException.class, () -> service.ensureObjectExists("key"));
    }

    // SdkClientExceptionが発生した場合、StorageExceptionにラップされることを確認
    @Test
    void ensureObjectExists_sdkClientException_wrappedAsStorageException() {
        when(client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
                .thenThrow(SdkClientException.create("client fail", null));

        assertThrows(MediaStorage.StorageException.class, () -> service.ensureObjectExists("key"));
    }

    // オブジェクト削除が成功する場合、例外が発生しないことを確認
    @Test
    void deleteObject_success_noException() {
        when(client.deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        assertDoesNotThrow(() -> service.deleteObject("some-key-to-delete"));
    }

    // S3Exceptionが発生した場合、StorageExceptionにラップされることを確認
    @Test
    void deleteObject_s3Exception_wrappedAsStorageException() {
        when(client.deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("delete failed").build());

        assertThrows(MediaStorage.StorageException.class, () -> service.deleteObject("key"));
    }

    // SdkClientExceptionが発生した場合、StorageExceptionにラップされることを確認
    @Test
    void deleteObject_sdkClientException_wrappedAsStorageException() {
        when(client.deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("delete fail", null));

        assertThrows(MediaStorage.StorageException.class, () -> service.deleteObject("key"));
    }
}
