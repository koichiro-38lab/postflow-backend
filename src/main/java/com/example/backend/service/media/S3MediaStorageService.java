package com.example.backend.service.media;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.example.backend.config.MediaStorageProperties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * AWS S3/MinIO等のオブジェクトストレージと連携し、
 * 署名付きURL発行・存在検証・削除などのメディア操作を提供する実装クラス。
 * <p>
 * {@link MediaStorage}インターフェースのS3実装。
 * バケット名・認証情報・エンドポイント等はMediaStorageProperties経由で注入。
 * <ul>
 * <li>管理画面・API経由の画像/ファイルアップロード・ダウンロード用途</li>
 * <li>署名付きURLは有効期限付きで発行</li>
 * <li>MinIO等のS3互換ストレージにも対応</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
public class S3MediaStorageService implements MediaStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaStorageProperties properties;
    private final Clock clock;

    /**
     * S3に対し署名付きアップロードURLを発行する。
     * 
     * @param storageKey    ストレージ内の保存先キー
     * @param contentType   Content-Type
     * @param contentLength バイト長
     * @param ttl           URL有効期間
     * @return PresignedUpload情報
     * @throws IllegalArgumentException 引数不正時
     * @throws StorageException         S3連携失敗時
     */
    @Override
    public PresignedUpload createUploadUrl(String storageKey, String contentType, long contentLength, Duration ttl) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey must be provided");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must be provided");
        }
        if (contentLength <= 0) {
            throw new IllegalArgumentException("contentLength must be greater than 0");
        }
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(ttl)
                .build();

        try {
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

            Map<String, String> headers = Map.of("Content-Type", contentType);
            Instant expiresAt = presigned.expiration() != null ? presigned.expiration() : Instant.now(clock).plus(ttl);

            return new PresignedUpload(presigned.url().toExternalForm(), headers, expiresAt, storageKey);
        } catch (S3Exception | SdkClientException e) {
            throw new StorageException("Failed to create presigned upload URL", e);
        }
    }

    /**
     * S3に対し署名付きダウンロードURLを発行する。
     * 
     * @param storageKey ストレージ内の保存先キー
     * @param ttl        URL有効期間
     * @return PresignedDownload情報
     * @throws IllegalArgumentException 引数不正時
     * @throws StorageException         S3連携失敗時
     */
    @Override
    public PresignedDownload createDownloadUrl(String storageKey, Duration ttl) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey must be provided");
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(b -> b.bucket(properties.getBucket()).key(storageKey).build())
                .build();
        try {
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            Instant expiresAt = presigned.expiration() != null ? presigned.expiration() : Instant.now(clock).plus(ttl);
            return new PresignedDownload(presigned.url().toExternalForm(), expiresAt);
        } catch (S3Exception | SdkClientException e) {
            throw new StorageException("Failed to create presigned download URL", e);
        }
    }

    /**
     * 指定キーのオブジェクトがS3上に存在するか検証する。
     * 
     * @param storageKey ストレージ内の保存先キー
     * @throws ObjectNotFoundException オブジェクトが存在しない場合
     * @throws StorageException        S3連携失敗時
     */
    @Override
    public void ensureObjectExists(String storageKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ObjectNotFoundException(storageKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ObjectNotFoundException(storageKey, e);
            }
            throw new StorageException("Failed to validate uploaded media", e);
        } catch (SdkClientException e) {
            throw new StorageException("Failed to communicate with S3", e);
        }
    }

    /**
     * 指定キーのオブジェクトをS3から削除する。
     * 
     * @param storageKey ストレージ内の保存先キー
     * @throws StorageException S3連携失敗時
     */
    @Override
    public void deleteObject(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
        } catch (S3Exception | SdkClientException e) {
            throw new StorageException("Failed to delete media object: " + storageKey, e);
        }
    }

    /**
     * S3ClientをMediaStoragePropertiesから構築するユーティリティ。
     * <ul>
     * <li>region, endpoint, pathStyle, credentials等を反映</li>
     * <li>MinIO等のS3互換ストレージにも対応</li>
     * </ul>
     * 
     * @param properties ストレージ設定
     * @return S3Clientインスタンス
     * @throws IllegalArgumentException 設定不正時
     */
    public static S3Client buildClient(MediaStorageProperties properties) {
        if (properties == null)
            throw new IllegalArgumentException("properties must not be null");
        if (properties.getRegion() == null || properties.getRegion().isBlank()) {
            throw new IllegalArgumentException("S3 region must be provided in MediaStorageProperties");
        }
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()));

        AwsCredentialsProvider credentials = resolveCredentials(properties);
        if (credentials != null) {
            builder.credentialsProvider(credentials);
        }

        if (properties.getEndpoint() != null) {
            builder.endpointOverride(properties.getEndpoint());
        }
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build();
        builder.serviceConfiguration(serviceConfiguration);

        return builder.build();
    }

    /**
     * S3PresignerをMediaStoragePropertiesから構築するユーティリティ。
     * <ul>
     * <li>region, endpoint, pathStyle, credentials等を反映</li>
     * <li>MinIO等のS3互換ストレージにも対応</li>
     * </ul>
     * 
     * @param properties ストレージ設定
     * @return S3Presignerインスタンス
     * @throws IllegalArgumentException 設定不正時
     */
    public static S3Presigner buildPresigner(MediaStorageProperties properties) {
        if (properties == null)
            throw new IllegalArgumentException("properties must not be null");
        if (properties.getRegion() == null || properties.getRegion().isBlank()) {
            throw new IllegalArgumentException("S3 region must be provided in MediaStorageProperties");
        }
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()));

        AwsCredentialsProvider credentials = resolveCredentials(properties);
        if (credentials != null) {
            builder.credentialsProvider(credentials);
        }
        if (properties.getEndpoint() != null) {
            builder.endpointOverride(properties.getEndpoint());
        }
        builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build());
        return builder.build();
    }

    /**
     * MediaStoragePropertiesから認証情報を解決するユーティリティ。
     * <ul>
     * <li>accessKey/secretKey指定時はStatic、未指定時はDefaultProvider</li>
     * </ul>
     * 
     * @param properties ストレージ設定
     * @return AwsCredentialsProvider
     */
    private static AwsCredentialsProvider resolveCredentials(MediaStorageProperties properties) {
        if (properties.getAccessKey() != null && properties.getSecretKey() != null) {
            AwsBasicCredentials basic = AwsBasicCredentials.create(properties.getAccessKey(),
                    properties.getSecretKey());
            return StaticCredentialsProvider.create(basic);
        }
        return DefaultCredentialsProvider.create();
    }
}
