package com.example.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.backend.config.MediaStorageProperties;
import com.example.backend.dto.media.MediaCreateRequestDto;
import com.example.backend.dto.media.MediaDownloadResponseDto;
import com.example.backend.dto.media.MediaPresignRequestDto;
import com.example.backend.dto.media.MediaPresignResponseDto;
import com.example.backend.dto.media.MediaResponseDto;
import com.example.backend.dto.media.MediaMapper;
import com.example.backend.entity.Media;
import com.example.backend.entity.User;
import com.example.backend.exception.MediaInUseException;
import com.example.backend.exception.MediaNotFoundException;
import com.example.backend.repository.MediaRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.security.MediaPolicy;
import com.example.backend.service.media.MediaStorage;
import com.example.backend.service.media.MediaStorage.ObjectNotFoundException;
import com.example.backend.service.media.MediaStorage.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final PostRepository postRepository;
    private final MediaPolicy mediaPolicy;
    private final MediaStorage mediaStorage;
    private final MediaStorageProperties mediaProperties;
    private final Clock clock;
    private final MediaMapper mediaMapper;

    @Transactional
    public MediaPresignResponseDto requestUpload(MediaPresignRequestDto dto, User currentUser) {
        mediaPolicy.checkCreate(currentUser.getRole(), currentUser.getId(), null, currentUser.getId());

        String storageKey = generateUniqueStorageKey(dto.getFilename());
        Duration ttl = resolveTtl();
        var presign = mediaStorage.createUploadUrl(storageKey, dto.getMime(), dto.getBytes(), ttl);

        return MediaPresignResponseDto.builder()
                .uploadUrl(presign.url())
                .storageKey(presign.storageKey())
                .headers(presign.headers())
                .expiresAt(presign.expiresAt())
                .build();
    }

    @Transactional
    public MediaResponseDto register(MediaCreateRequestDto dto, User currentUser) {
        mediaPolicy.checkCreate(currentUser.getRole(), currentUser.getId(), null, currentUser.getId());

        if (mediaRepository.existsByStorageKey(dto.getStorageKey())) {
            throw new MediaInUseException("error.media.storageKey.duplicate");
        }

        try {
            mediaStorage.ensureObjectExists(dto.getStorageKey());
        } catch (ObjectNotFoundException e) {
            throw new IllegalArgumentException("Uploaded object not found for storageKey: " + dto.getStorageKey());
        } catch (StorageException e) {
            throw new IllegalStateException("Failed to validate uploaded media", e);
        }

        Media media = Media.builder()
                .filename(dto.getFilename())
                .storageKey(dto.getStorageKey())
                .mime(dto.getMime())
                .bytes(dto.getBytes())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .altText(dto.getAltText())
                .createdBy(currentUser)
                .build();

        Media saved = mediaRepository.save(media);
        return mediaMapper.toResponseDto(saved, buildPublicUrl(saved));
    }

    @Transactional(readOnly = true)
    public Page<MediaResponseDto> list(String mime, String keyword, Pageable pageable, User currentUser) {
        Specification<Media> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(mime)) {
            String normalized = mime.toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("mime")), normalized + "%"));
        }
        if (StringUtils.hasText(keyword)) {
            String normalized = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("filename")), normalized));
        }
        if (currentUser.getRole() == User.Role.AUTHOR) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("createdBy").get("id"), currentUser.getId()));
        }

        return mediaRepository.findAll(spec, pageable)
                .map(media -> mediaMapper.toResponseDto(media, buildPublicUrl(media)));
    }

    @Transactional(readOnly = true)
    public MediaResponseDto getById(Long id, User currentUser) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new MediaNotFoundException(id));
        mediaPolicy.checkRead(currentUser.getRole(), media.getCreatedBy().getId(), null, currentUser.getId());
        return mediaMapper.toResponseDto(media, buildPublicUrl(media));
    }

    @Transactional
    public MediaDownloadResponseDto createDownloadUrl(Long id, User currentUser) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new MediaNotFoundException(id));
        mediaPolicy.checkRead(currentUser.getRole(), media.getCreatedBy().getId(), null, currentUser.getId());

        Duration ttl = resolveTtl();
        var presigned = mediaStorage.createDownloadUrl(media.getStorageKey(), ttl);
        return MediaDownloadResponseDto.builder()
                .downloadUrl(presigned.url())
                .expiresAt(presigned.expiresAt())
                .build();
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new MediaNotFoundException(id));
        mediaPolicy.checkDelete(currentUser.getRole(), media.getCreatedBy().getId(), null, currentUser.getId());

        if (postRepository.existsByCoverMediaId(media.getId())) {
            throw new MediaInUseException("error.media.inUse.cover");
        }

        mediaRepository.delete(media);
        try {
            mediaStorage.deleteObject(media.getStorageKey());
        } catch (StorageException e) {
            // Object deletion failures should not rollback DB deletion, but notify clients.
            throw new IllegalStateException("Failed to delete media object from storage", e);
        }
    }

    private Duration resolveTtl() {
        return mediaProperties.getPresignTtl() != null ? mediaProperties.getPresignTtl() : Duration.ofMinutes(15);
    }

    private String generateUniqueStorageKey(String filename) {
        String key;
        int attempts = 0;
        do {
            key = buildStorageKey(filename);
            attempts++;
            if (attempts > 5) {
                throw new IllegalStateException("Failed to generate unique storage key");
            }
        } while (mediaRepository.existsByStorageKey(key));
        return key;
    }

    private String buildStorageKey(String filename) {
        LocalDateTime now = LocalDateTime.now(clock);
        String extension = extractExtension(filename);
        String basePath = String.format("%d/%02d/%s%s", now.getYear(), now.getMonthValue(), UUID.randomUUID(),
                extension);

        String prefix = mediaProperties.getKeyPrefix();
        if (StringUtils.hasText(prefix)) {
            prefix = trimSlashes(prefix);
        } else {
            prefix = null;
        }

        String path = prefix != null && !prefix.isEmpty() ? prefix + "/" + basePath : basePath;
        return path;
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx <= 0 || idx == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(idx).toLowerCase(Locale.ROOT);
        return ext;
    }

    private String trimSlashes(String value) {
        String v = value;
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private String buildPublicUrl(Media media) {
        if (mediaProperties.getPublicBaseUrl() == null) {
            return null;
        }
        String base = mediaProperties.getPublicBaseUrl().toString();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + media.getStorageKey();
    }
}
