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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostPolicy postPolicy;
    private final TagService tagService;
    private final Clock clock;

    // 投稿のアクセス権をチェックし、権限がなければ例外をスロー
    public void checkAccessOrThrow(Post post, Long currentUserId, User.Role userRole) {
        Long authorId = (post.getAuthor() != null) ? post.getAuthor().getId() : null;
        postPolicy.checkRead(userRole, authorId, currentUserId);
    }

    // 全件取得
    @Transactional(readOnly = true)
    public Page<PostResponseDto> findAll(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(post -> postMapper.toResponseDto(post, false));
    }

    // 検索
    @Transactional(readOnly = true)
    public Page<PostResponseDto> search(String title, String slug, String status, Long authorId, Long categoryId,
            List<String> tagSlugs, Pageable pageable) {
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
                /* ignore invalid status */ }
        }
        if (authorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("author").get("id"), authorId));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        List<String> normalizedTagSlugs = tagSlugs != null
                ? tagSlugs.stream().map(tagService::normalizeSlug).toList()
                : List.of();
        if (!normalizedTagSlugs.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Objects.requireNonNull(query, "query must not be null");
                query.distinct(true);
                var tagsJoin = root.join("tags");
                return tagsJoin.get("slug").in(normalizedTagSlugs);
            });
        }

        return postRepository.findAll(spec, pageable).map(postMapper::toResponseDto);
    }

    // 投稿を1件をIDで取得し、アクセス権をチェック
    @Transactional(readOnly = true)
    public Optional<PostResponseDto> findById(Long id, Long currentUserId, User.Role userRole) {
        return postRepository.findById(id).map(post -> {
            checkAccessOrThrow(post, currentUserId, userRole);
            return postMapper.toResponseDto(post);
        });
    }

    // 投稿を1件をIDで取得し、アクセス権をチェック（管理者権限で実行）
    @Transactional(readOnly = true)
    public Optional<PostResponseDto> findById(Long id) {
        return postRepository.findById(id)
                .map(post -> postMapper.toResponseDto(post, true));
    }

    // 投稿を作成
    @Transactional
    public PostResponseDto create(PostRequestDto dto, Long currentUserId, User.Role userRole) {
        postPolicy.checkCreate(userRole, dto.getAuthorId(), currentUserId);
        Post post = new Post();
        postMapper.applyToEntity(post, dto);
        applyTags(post, dto.getTags());
        Post saved = postRepository.save(post);
        return postMapper.toResponseDto(saved);
    }

    // 投稿を作成（管理者権限で実行）
    @Transactional
    public PostResponseDto create(PostRequestDto dto) {
        return create(dto, 1L, User.Role.ADMIN);
    }

    // 投稿を1件更新し、アクセス権をチェック
    @Transactional
    public Optional<PostResponseDto> update(Long id, PostRequestDto dto, Long currentUserId, User.Role userRole) {
        return postRepository.findById(id).map(post -> {
            Long authorId = (post.getAuthor() != null) ? post.getAuthor().getId() : null;
            postPolicy.checkUpdate(userRole, authorId, dto.getAuthorId(), currentUserId, dto);
            postMapper.applyToEntity(post, dto);
            applyTags(post, dto.getTags());
            if ("PUBLISHED".equals(dto.getStatus()) && post.getPublishedAt() == null) {
                post.setPublishedAt(LocalDateTime.now(clock));
            }
            return postMapper.toResponseDto(post);
        });
    }

    // 投稿を1件更新し、アクセス権をチェック（管理者権限で実行）
    @Transactional
    public Optional<PostResponseDto> update(Long id, PostRequestDto dto) {
        return update(id, dto, 1L, User.Role.ADMIN);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, User.Role userRole) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.example.backend.exception.PostNotFoundException(id));
        Long authorId = (post.getAuthor() != null) ? post.getAuthor().getId() : null;
        postPolicy.checkDelete(userRole, authorId, currentUserId);
        postRepository.deleteById(id);
    }

    // Backward-compatible: treat as ADMIN internally
    @Transactional
    public void delete(Long id) {
        delete(id, 1L, User.Role.ADMIN);
    }

    private void applyTags(Post post, List<String> tagSlugs) {
        if (tagSlugs == null) {
            if (post.getTags() == null) {
                post.setTags(new ArrayList<>());
            }
            return;
        }
        List<Tag> tags = tagService.findAllBySlugs(tagSlugs);
        post.setTags(new ArrayList<>(tags));
    }
}
