package com.example.backend.config;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.backend.service.media.MediaStorage;

@TestConfiguration
public class FakeMediaStorageConfig {

    @Bean
    @Primary
    public InMemoryMediaStorage mediaStorage(Clock clock) {
        return new InMemoryMediaStorage(clock);
    }

    public static class InMemoryMediaStorage implements MediaStorage {

        private final Clock clock;
        private final Set<String> uploaded = ConcurrentHashMap.newKeySet();

        public InMemoryMediaStorage(Clock clock) {
            this.clock = clock;
        }

        @Override
        public PresignedUpload createUploadUrl(String storageKey, String contentType, long contentLength,
                Duration ttl) {
            Instant expiresAt = Instant.now(clock).plus(ttl);
            return new PresignedUpload("http://localhost/upload/" + storageKey,
                    Map.of("Content-Type", contentType), expiresAt, storageKey);
        }

        @Override
        public PresignedDownload createDownloadUrl(String storageKey, Duration ttl) {
            if (!uploaded.contains(storageKey)) {
                throw new ObjectNotFoundException(storageKey);
            }
            Instant expiresAt = Instant.now(clock).plus(ttl);
            return new PresignedDownload("http://localhost/download/" + storageKey, expiresAt);
        }

        @Override
        public void ensureObjectExists(String storageKey) throws ObjectNotFoundException {
            if (!uploaded.contains(storageKey)) {
                throw new ObjectNotFoundException(storageKey);
            }
        }

        @Override
        public void deleteObject(String storageKey) {
            uploaded.remove(storageKey);
        }

        public void simulateUpload(String storageKey) {
            uploaded.add(storageKey);
        }
    }
}
