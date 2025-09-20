package com.example.backend.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media")
public class MediaStorageProperties {

    /**
     * The S3 bucket name used for storing media objects.
     */
    private String bucket;

    /**
     * AWS region (or virtual region when using MinIO) for S3 requests.
     */
    private String region;

    /**
     * Optional custom endpoint (e.g. http://localhost:9000 for MinIO).
     */
    private URI endpoint;

    /**
     * Optional access key/secret key pair when custom credentials are required.
     */
    private String accessKey;
    private String secretKey;

    /**
     * Whether to force path-style access. Enabled by default for MinIO.
     */
    private boolean pathStyleAccess = true;

    /**
     * Default expiry for presigned URLs.
     */
    private Duration presignTtl = Duration.ofMinutes(15);

    /**
     * Optional public base URL (e.g. CloudFront) used when building public URLs.
     */
    private URI publicBaseUrl;

    /**
     * Optional folder prefix added to every generated storage key.
     */
    private String keyPrefix = "media";
}
