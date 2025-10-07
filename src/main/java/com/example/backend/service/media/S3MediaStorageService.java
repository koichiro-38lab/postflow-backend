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

@RequiredArgsConstructor
public class S3MediaStorageService implements MediaStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaStorageProperties properties;
    private final Clock clock;

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

    private static AwsCredentialsProvider resolveCredentials(MediaStorageProperties properties) {
        if (properties.getAccessKey() != null && properties.getSecretKey() != null) {
            AwsBasicCredentials basic = AwsBasicCredentials.create(properties.getAccessKey(),
                    properties.getSecretKey());
            return StaticCredentialsProvider.create(basic);
        }
        return DefaultCredentialsProvider.create();
    }
}
