package com.example.backend.dto.post;

import com.example.backend.entity.Category;
import com.example.backend.entity.Media;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.entity.Tag;
import com.example.backend.config.AppProperties;
import com.example.backend.config.MediaStorageProperties;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.MediaRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final CategoryRepository categoryRepository;
    private final MediaStorageProperties mediaProperties;
    private final AppProperties appProperties;

    public PostResponseDto toResponseDto(Post post) {
        return toResponseDto(post, true);
    }

    // Detailed DTO変換（contentJson含む）
    public PostResponseDto toResponseDto(Post post, boolean includeContentJson) {
        PostResponseDto.PostResponseDtoBuilder builder = PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .status(post.getStatus().name())
                .excerpt(post.getExcerpt());
        if (includeContentJson) {
            builder.contentJson(post.getContentJson());
        }
        builder.coverMedia(post.getCoverMedia() != null ? CoverMediaSummaryDto.builder()
                .id(post.getCoverMedia().getId())
                .filename(post.getCoverMedia().getFilename())
                .storageKey(post.getCoverMedia().getStorageKey())
                .mime(post.getCoverMedia().getMime())
                .bytes(post.getCoverMedia().getBytes())
                .width(post.getCoverMedia().getWidth())
                .height(post.getCoverMedia().getHeight())
                .altText(post.getCoverMedia().getAltText() != null ? post.getCoverMedia().getAltText() : "")
                .build() : null)
                .author(post.getAuthor() != null ? AuthorSummaryDto.builder()
                        .id(post.getAuthor().getId())
                        .displayName(post.getAuthor().getDisplayName())
                        .avatarUrl(post.getAuthor().getAvatarMedia() != null
                                ? buildPublicUrl(post.getAuthor().getAvatarMedia())
                                : null)
                        .bio(post.getAuthor().getBio())
                        .build() : null)
                .category(post.getCategory() != null ? CategorySummaryDto.builder()
                        .id(post.getCategory().getId())
                        .name(post.getCategory().getName())
                        .slug(post.getCategory().getSlug())
                        .build() : null)
                .tags(post.getTags() != null
                        ? post.getTags().stream()
                                .map((Tag tag) -> TagSummaryDto.builder()
                                        .id(tag.getId())
                                        .name(tag.getName())
                                        .slug(tag.getSlug())
                                        .build())
                                .toList()
                        : List.of())
                .publishedAt(toOffsetDateTime(post.getPublishedAt()))
                .createdAt(toOffsetDateTime(post.getCreatedAt()))
                .updatedAt(toOffsetDateTime(post.getUpdatedAt()));
        return builder.build();
    }

    // EntityにDTOの内容を適用
    public void applyToEntity(Post post, PostRequestDto dto) {
        post.setTitle(dto.getTitle());
        post.setSlug(dto.getSlug());
        post.setStatus(Post.Status.valueOf(dto.getStatus()));
        post.setExcerpt(dto.getExcerpt());
        setContentJson(post, dto.getContentJson());
        if (dto.getAuthorId() != null)
            setAuthor(post, dto.getAuthorId());
        setCoverMedia(post, dto.getCoverMediaId());
        setCategory(post, dto.getCategoryId());
        setPublishedAt(post, dto.getPublishedAt());
    }

    // JSON文字列を検証して設定
    private void setContentJson(Post post, String contentJson) {
        try {
            post.setContentJson(objectMapper.readTree(contentJson).toString());
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON for contentJson", e);
        }
    }

    // authorIdがnullでない場合に設定
    private void setAuthor(Post post, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found: " + authorId));
        post.setAuthor(author);
    }

    // coverMediaIdがnullの場合はnullを設定
    private void setCoverMedia(Post post, Long coverMediaId) {
        if (coverMediaId != null) {
            Media media = mediaRepository.findById(coverMediaId)
                    .orElseThrow(() -> new IllegalArgumentException("Media not found: " + coverMediaId));
            post.setCoverMedia(media);
        } else {
            post.setCoverMedia(null);
        }
    }

    // categoryIdがnullの場合はnullを設定
    private void setCategory(Post post, Long categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
            post.setCategory(category);
        } else {
            post.setCategory(null);
        }
    }

    // publishedAtがnullの場合はnullを設定
    private void setPublishedAt(Post post, java.time.OffsetDateTime publishedAt) {
        if (publishedAt != null) {
            post.setPublishedAt(java.time.LocalDateTime.ofInstant(
                    publishedAt.toInstant(), java.time.ZoneOffset.UTC));
        } else {
            post.setPublishedAt(null);
        }
    }

    // LocalDateTime -> OffsetDateTime (UTC)
    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atOffset(ZoneOffset.UTC);
    }

    // Build public URL for media
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

    // 公開API用: 一覧表示用DTO変換
    public PostPublicResponseDto toPublicResponseDto(Post post) {

        return PostPublicResponseDto.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .excerpt(post.getExcerpt())
                .publishedAt(toOffsetDateTime(post.getPublishedAt()))
                .coverMedia(post.getCoverMedia() != null ? PostCoverMediaSummaryDto.builder()
                        .url(buildPublicUrl(post.getCoverMedia()))
                        .width(post.getCoverMedia().getWidth())
                        .height(post.getCoverMedia().getHeight())
                        .altText(post.getCoverMedia().getAltText() != null ? post.getCoverMedia().getAltText() : "")
                        .build() : null)
                .category(post.getCategory() != null ? CategorySummaryDto.builder()
                        .id(post.getCategory().getId())
                        .name(post.getCategory().getName())
                        .slug(post.getCategory().getSlug())
                        .build() : null)
                .tags(post.getTags() != null
                        ? post.getTags().stream()
                                .map((Tag tag) -> TagSummaryDto.builder()
                                        .id(tag.getId())
                                        .name(tag.getName())
                                        .slug(tag.getSlug())
                                        .build())
                                .toList()
                        : List.of())
                .build();
    }

    // 公開API用: 詳細表示用DTO変換（SEO/OGP情報含む）
    public PostPublicDetailResponseDto toPublicDetailResponseDto(Post post) {

        String baseUrl = appProperties.getBaseUrl();

        return PostPublicDetailResponseDto.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .excerpt(post.getExcerpt())
                .contentJson(post.getContentJson())
                .publishedAt(toOffsetDateTime(post.getPublishedAt()))
                .coverMedia(post.getCoverMedia() != null ? PostCoverMediaSummaryDto.builder()
                        .url(buildPublicUrl(post.getCoverMedia()))
                        .width(post.getCoverMedia().getWidth())
                        .height(post.getCoverMedia().getHeight())
                        .altText(post.getCoverMedia().getAltText() != null ? post.getCoverMedia().getAltText() : "")
                        .build() : null)
                .category(post.getCategory() != null ? CategorySummaryDto.builder()
                        .id(post.getCategory().getId())
                        .name(post.getCategory().getName())
                        .slug(post.getCategory().getSlug())
                        .build() : null)
                .tags(post.getTags() != null
                        ? post.getTags().stream()
                                .map((Tag tag) -> TagSummaryDto.builder()
                                        .id(tag.getId())
                                        .name(tag.getName())
                                        .slug(tag.getSlug())
                                        .build())
                                .toList()
                        : List.of())
                .author(post.getAuthor() != null ? AuthorSummaryDto.builder()
                        .id(post.getAuthor().getId())
                        .displayName(post.getAuthor().getDisplayName())
                        .avatarUrl(post.getAuthor().getAvatarMedia() != null
                                ? buildPublicUrl(post.getAuthor().getAvatarMedia())
                                : null)
                        .bio(post.getAuthor().getBio())
                        .build() : null)
                // OGPフィールド追加
                .ogTitle(post.getTitle())
                .ogDescription(post.getExcerpt())
                .ogImage(post.getCoverMedia() != null ? buildPublicUrl(post.getCoverMedia()) : null)
                .ogUrl(baseUrl + "/posts/" + post.getSlug())
                .build();
    }

}
