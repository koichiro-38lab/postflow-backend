
package com.example.backend.service;

import com.example.backend.dto.post.PostRequestDto;
import com.example.backend.dto.post.PostResponseDto;
import com.example.backend.dto.post.PostMapper;
import com.example.backend.entity.Post;
import com.example.backend.entity.Tag;
import com.example.backend.entity.User;
import com.example.backend.repository.PostRepository;
import com.example.backend.security.PostPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Clock;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostPolicy postPolicy;
    private final TagService tagService;
    private final Clock clock;

    // Controller用: RBAC・DTO変換まで一貫
    @Transactional(readOnly = true)
    public Optional<PostResponseDto> findByIdWithAccessControl(Long id, User user) {
        Optional<Post> postOpt = postRepository.findById(id);
        if (postOpt.isEmpty())
            return Optional.empty();
        Post post = postOpt.get();
        if (!canAccess(post, user)) {
            throw new com.example.backend.exception.AccessDeniedException("Access denied to this post");
        }
        return Optional.of(postMapper.toResponseDto(post));
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> searchWithAccessControl(
            String title, String slug, String status, Long authorId, Long categoryId, String tagParam,
            Pageable pageable, User user) {
        // タグパラメータの分割
        List<String> tagSlugs = (tagParam != null && !tagParam.isBlank())
                ? Arrays.stream(tagParam.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toList())
                : List.of();
        // 検索条件
        Specification<Post> spec = Specification.allOf();
        if (StringUtils.hasText(title)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
        }
        if (StringUtils.hasText(slug)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("slug"), slug));
        }
        if (StringUtils.hasText(status)) {
            try {
                Post.Status postStatus = Post.Status.valueOf(status);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), postStatus));
            } catch (IllegalArgumentException ignore) {
            }
        }
        if (authorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("author").get("id"), authorId));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (!tagSlugs.isEmpty()) {
            List<String> normalizedTagSlugs = tagSlugs.stream().map(tagService::normalizeSlug).toList();
            spec = spec.and((root, query, cb) -> {
                if (query != null) {
                    query.distinct(true);
                }
                var tagsJoin = root.join("tags");
                return tagsJoin.get("slug").in(normalizedTagSlugs);
            });
        }
        // ロールによる絞り込み
        if (user.getRole() == User.Role.AUTHOR) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("author").get("id"), user.getId()));
        }
        return postRepository.findAll(spec, pageable).map(postMapper::toResponseDto);
    }

    @Transactional
    public PostResponseDto create(PostRequestDto dto, User user) {
        postPolicy.checkCreate(user.getRole(), dto.getAuthorId(), null, user.getId());
        Post post = new Post();
        postMapper.applyToEntity(post, dto);
        applyTags(post, dto.getTags(), dto.getTagIds());
        Post saved = postRepository.save(post);
        return postMapper.toResponseDto(saved);
    }

    @Transactional
    public Optional<PostResponseDto> update(Long id, PostRequestDto dto, User user) {
        return postRepository.findById(id).map(post -> {
            Long authorId = (post.getAuthor() != null) ? post.getAuthor().getId() : null;
            if (user.getRole() == User.Role.AUTHOR && "PUBLISHED".equals(dto.getStatus())) {
                throw new com.example.backend.exception.AccessDeniedException("Authors cannot publish posts");
            }
            postPolicy.checkUpdate(user.getRole(), authorId, dto.getAuthorId(), user.getId());
            postMapper.applyToEntity(post, dto);
            applyTags(post, dto.getTags(), dto.getTagIds());
            if ("PUBLISHED".equals(dto.getStatus()) && post.getPublishedAt() == null) {
                post.setPublishedAt(LocalDateTime.now(clock));
            }
            return postMapper.toResponseDto(post);
        });
    }

    @Transactional
    public void delete(Long id, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.example.backend.exception.PostNotFoundException(id));
        Long authorId = (post.getAuthor() != null) ? post.getAuthor().getId() : null;
        postPolicy.checkDelete(user.getRole(), authorId, null, user.getId());
        postRepository.deleteById(id);
    }

    // 投稿のアクセス権判定（RBAC）
    private boolean canAccess(Post post, User user) {
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.EDITOR)
            return true;
        if (user.getRole() == User.Role.AUTHOR) {
            return post.getAuthor() != null && user.getId().equals(post.getAuthor().getId());
        }
        return false;
    }

    private void applyTags(Post post, List<String> tagSlugs, List<Long> tagIds) {
        List<Tag> tags = new ArrayList<>();
        if (tagSlugs != null && !tagSlugs.isEmpty()) {
            tags.addAll(tagService.findAllBySlugs(tagSlugs));
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            tags.addAll(tagService.findAllByIds(tagIds));
        }
        post.setTags(tags);
    }

}
