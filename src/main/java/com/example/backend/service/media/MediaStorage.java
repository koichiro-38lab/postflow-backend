package com.example.backend.service.media;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface MediaStorage {

    record PresignedUpload(String url, Map<String, String> headers, Instant expiresAt, String storageKey) {
    }

    record PresignedDownload(String url, Instant expiresAt) {
    }

    PresignedUpload createUploadUrl(String storageKey, String contentType, long contentLength, Duration ttl);

    PresignedDownload createDownloadUrl(String storageKey, Duration ttl);

    void ensureObjectExists(String storageKey) throws ObjectNotFoundException;

    void deleteObject(String storageKey);

    class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }

        public StorageException(String message) {
            super(message);
        }
    }

    class ObjectNotFoundException extends StorageException {
        public ObjectNotFoundException(String storageKey, Throwable cause) {
            super("Media object not found for key: " + storageKey, cause);
        }

        public ObjectNotFoundException(String storageKey) {
            super("Media object not found for key: " + storageKey);
        }
    }
}
