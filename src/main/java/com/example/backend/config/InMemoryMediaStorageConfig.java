package com.example.backend.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.backend.service.media.MediaStorage;

@Configuration
@ConditionalOnProperty(prefix = "app.media", name = "use-in-memory", havingValue = "true", matchIfMissing = true)
public class InMemoryMediaStorageConfig {

    @Bean
    public MediaStorage inMemoryMediaStorage(Clock clock) {
        return new InMemoryMediaStorage(clock);
    }

    static class InMemoryMediaStorage implements MediaStorage {

        private final Clock clock;
        private final Set<String> existingKeys = ConcurrentHashMap.newKeySet();

        InMemoryMediaStorage(Clock clock) {
            this.clock = clock;
        }

        @Override
        public PresignedUpload createUploadUrl(String storageKey, String contentType, long contentLength,
                Duration ttl) {
            existingKeys.add(storageKey);
            Instant expiresAt = Instant.now(clock).plus(ttl);
            return new PresignedUpload("http://localhost/mock-upload/" + storageKey,
                    Map.of("Content-Type", contentType), expiresAt, storageKey);
        }

        @Override
        public PresignedDownload createDownloadUrl(String storageKey, Duration ttl) {
            if (!existingKeys.contains(storageKey)) {
                throw new ObjectNotFoundException(storageKey);
            }
            Instant expiresAt = Instant.now(clock).plus(ttl);
            return new PresignedDownload("http://localhost/mock-download/" + storageKey, expiresAt);
        }

        @Override
        public void ensureObjectExists(String storageKey) {
            if (!existingKeys.contains(storageKey)) {
                throw new ObjectNotFoundException(storageKey);
            }
        }

        @Override
        public void deleteObject(String storageKey) {
            existingKeys.remove(storageKey);
        }
    }
}
