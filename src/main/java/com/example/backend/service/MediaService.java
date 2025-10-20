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

/**
 * メディア管理サービス。
 * <p>
 * メディアのアップロード・登録・一覧・詳細・ダウンロードURL発行・削除を提供。全操作でRBAC・参照整合性・ストレージ整合性を考慮。
 * <ul>
 * <li>アップロード: presign URL発行、ストレージ一意キー生成</li>
 * <li>登録: presign後のメタ情報登録、存在検証</li>
 * <li>一覧: MIME/キーワード/ページング・RBAC対応</li>
 * <li>詳細/ダウンロード: アクセス権・一時URL発行</li>
 * <li>削除: 投稿参照時は例外(MediaInUseException)、ストレージ削除失敗時は通知</li>
 * </ul>
 * 
 * @see com.example.backend.repository.MediaRepository
 * @see com.example.backend.security.MediaPolicy
 */
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

    /**
     * メディアアップロード用の事前署名URLを発行。
     * <p>
     * RBAC制御あり。ストレージ一意キー生成・presign URL発行。
     * </p>
     * 
     * @param dto         アップロードリクエスト情報
     * @param currentUser 操作ユーザー
     * @return 事前署名URL情報
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
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

    /**
     * presign後のメディア情報を登録。
     * <p>
     * RBAC制御あり。ストレージ上の存在検証・重複チェック。
     * </p>
     * 
     * @param dto         登録リクエスト情報
     * @param currentUser 操作ユーザー
     * @return 登録済みメディア情報
     * @throws com.example.backend.exception.MediaInUseException storageKey重複時
     * @throws java.lang.IllegalArgumentException                ストレージ上にオブジェクトが存在しない場合
     * @throws java.lang.IllegalStateException                   ストレージ検証失敗時
     */
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

    /**
     * メディア一覧を取得。
     * <p>
     * MIME/キーワードでフィルタ、ページング・RBAC対応。
     * </p>
     * 
     * @param mime        MIMEタイプ（任意）
     * @param keyword     検索キーワード（任意）
     * @param pageable    ページ情報
     * @param currentUser 操作ユーザー
     * @return メディア情報ページ
     */
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

    /**
     * ID指定でメディア詳細を取得。
     * <p>
     * RBAC制御あり。存在しない場合は例外。
     * </p>
     * 
     * @param id          メディアID
     * @param currentUser 操作ユーザー
     * @return メディア情報
     * @throws com.example.backend.exception.MediaNotFoundException 存在しない場合
     * @throws com.example.backend.exception.AccessDeniedException  権限不足
     */
    @Transactional(readOnly = true)
    public MediaResponseDto getById(Long id, User currentUser) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new MediaNotFoundException(id));
        mediaPolicy.checkReadForMedia(currentUser.getRole(), media.getId(), media.getCreatedBy().getId(),
                currentUser.getId());
        return mediaMapper.toResponseDto(media, buildPublicUrl(media));
    }

    /**
     * メディアダウンロード用の一時URLを発行。
     * <p>
     * RBAC制御あり。存在しない場合は例外。
     * </p>
     * 
     * @param id          メディアID
     * @param currentUser 操作ユーザー
     * @return ダウンロードURL情報
     * @throws com.example.backend.exception.MediaNotFoundException 存在しない場合
     * @throws com.example.backend.exception.AccessDeniedException  権限不足
     */
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

    /**
     * メディアを削除。
     * <p>
     * RBAC制御あり。投稿で参照されている場合はMediaInUseException。ストレージ削除失敗時は通知。
     * </p>
     * 
     * @param id          メディアID
     * @param currentUser 操作ユーザー
     * @throws com.example.backend.exception.MediaNotFoundException 存在しない場合
     * @throws com.example.backend.exception.MediaInUseException    投稿参照時
     * @throws java.lang.IllegalStateException                      ストレージ削除失敗時
     */
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

    /**
     * 事前署名URLの有効期限を解決。
     * <p>
     * 設定値がなければ15分。
     * </p>
     * 
     * @return presign URLの有効期限
     */
    private Duration resolveTtl() {
        return mediaProperties.getPresignTtl() != null ? mediaProperties.getPresignTtl() : Duration.ofMinutes(15);
    }

    /**
     * 一意なストレージキーを生成。
     * <p>
     * 5回まで重複チェック。
     * </p>
     * 
     * @param filename 元のファイル名
     * @return 一意なストレージキー
     * @throws java.lang.IllegalStateException 5回重複時
     */
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

    /**
     * ストレージキーを構築。
     * <p>
     * 年/月/UUID+拡張子形式。
     * </p>
     * 
     * @param filename 元のファイル名
     * @return 一意なストレージキー
     */
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

    /**
     * ファイル名から拡張子を抽出。
     * <p>
     * ドット付き小文字。拡張子がない場合は空文字列。
     * </p>
     * 
     * @param filename 元のファイル名
     * @return 拡張子（ドット付き、小文字）
     */
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

    /**
     * 文字列の前後のスラッシュを削除。
     * <p>
     * S3キーprefix整形用。
     * </p>
     * 
     * @param value 入力文字列
     * @return 前後のスラッシュが削除された文字列
     */
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

    /**
     * メディアの公開URLを構築。
     * <p>
     * mediaProperties.getPublicBaseUrl()がnullの場合はnull。
     * </p>
     * 
     * @param media メディアエンティティ
     * @return 公開URL
     */
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
