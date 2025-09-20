package com.example.backend.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.backend.service.media.MediaStorage;
import com.example.backend.service.media.S3MediaStorageService;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(prefix = "app.media", name = { "bucket", "region" })
public class S3MediaStorageConfig {

    @Bean
    public S3Client s3Client(MediaStorageProperties properties) {
        return S3MediaStorageService.buildClient(properties);
    }

    @Bean
    public S3Presigner s3Presigner(MediaStorageProperties properties) {
        return S3MediaStorageService.buildPresigner(properties);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(MediaStorage.class)
    public MediaStorage mediaStorage(S3Client s3Client, S3Presigner s3Presigner, MediaStorageProperties properties,
            Clock clock) {
        return new S3MediaStorageService(s3Client, s3Presigner, properties, clock);
    }
}
