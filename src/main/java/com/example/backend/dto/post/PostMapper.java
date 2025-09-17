package com.example.backend.dto.post;

import com.example.backend.entity.Category;
import com.example.backend.entity.Media;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.MediaRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final CategoryRepository categoryRepository;

    public PostResponseDto toResponseDto(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .status(post.getStatus().name())
                .excerpt(post.getExcerpt())
                .contentJson(post.getContentJson())
                .coverMediaId(post.getCoverMedia() != null ? post.getCoverMedia().getId() : null)
                .authorId(post.getAuthor() != null ? post.getAuthor().getId() : null)
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // Mutates the given entity from request DTO (create/update)
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

    private void setContentJson(Post post, String contentJson) {
        try {
            post.setContentJson(objectMapper.readTree(contentJson).toString());
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON for contentJson", e);
        }
    }

    private void setAuthor(Post post, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found: " + authorId));
        post.setAuthor(author);
    }

    private void setCoverMedia(Post post, Long coverMediaId) {
        if (coverMediaId != null) {
            Media media = mediaRepository.findById(coverMediaId)
                    .orElseThrow(() -> new IllegalArgumentException("Media not found: " + coverMediaId));
            post.setCoverMedia(media);
        } else {
            post.setCoverMedia(null);
        }
    }

    private void setCategory(Post post, Long categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
            post.setCategory(category);
        } else {
            post.setCategory(null);
        }
    }

    private void setPublishedAt(Post post, String publishedAt) {
        if (publishedAt != null && !publishedAt.isBlank()) {
            post.setPublishedAt(java.time.LocalDateTime.parse(publishedAt));
        } else {
            post.setPublishedAt(null);
        }
    }
}
