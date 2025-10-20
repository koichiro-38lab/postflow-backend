
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

/**
 * 投稿管理サービス。
 * <p>
 * 投稿の検索・詳細・作成・更新・削除を提供。全操作でRBAC・参照整合性を考慮。
 * <ul>
 * <li>検索: タイトル・スラッグ・ステータス・著者・カテゴリ・タグ・ページング・RBAC対応</li>
 * <li>詳細: ID指定・RBAC対応</li>
 * <li>作成: RBAC・著者ID必須・タグ付与</li>
 * <li>更新: RBAC・タグ・公開日制御</li>
 * <li>削除: RBAC・存在しない場合は例外</li>
 * </ul>
 * 
 * @see com.example.backend.repository.PostRepository
 * @see com.example.backend.security.PostPolicy
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostPolicy postPolicy;
    private final TagService tagService;
    private final Clock clock;

    /**
     * ID指定で投稿詳細を取得（RBAC制御付き）。
     * <p>
     * ADMIN/EDITORは全件、AUTHORは自分の投稿のみ取得可能。存在しない場合は空Optional。
     * </p>
     * 
     * @param id   投稿ID
     * @param user 取得ユーザー
     * @return 投稿詳細DTO（Optional）
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
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

    /**
     * 投稿を検索（RBAC制御付き）。
     * <p>
     * タイトル・スラッグ・ステータス・著者・カテゴリ・タグでフィルタ、ページング・RBAC対応。
     * </p>
     * 
     * @param title      タイトル（任意）
     * @param slug       スラッグ（任意）
     * @param status     ステータス（任意）
     * @param authorId   著者ID（任意）
     * @param categoryId カテゴリID（任意）
     * @param tagParam   タグ（カンマ区切り, 任意）
     * @param pageable   ページング情報
     * @param user       検索ユーザー
     * @return 投稿ページ
     */
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

    /**
     * 投稿を新規作成。
     * <p>
     * RBAC制御あり。authorId必須。タグ付与・公開日制御。
     * </p>
     * 
     * @param dto  投稿作成リクエスト
     * @param user 作成ユーザー
     * @return 作成された投稿詳細DTO
     * @throws java.lang.IllegalArgumentException                  authorId未指定時
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
    @Transactional
    public PostResponseDto create(PostRequestDto dto, User user) {
        if (dto.getAuthorId() == null) {
            throw new IllegalArgumentException("authorId is required for post creation");
        }
        postPolicy.checkCreate(user.getRole(), dto.getAuthorId(), null, user.getId());
        Post post = new Post();
        postMapper.applyToEntity(post, dto);

        // 過去の日付が設定されている場合はnullに設定
        if (post.getPublishedAt() != null && post.getPublishedAt().isBefore(LocalDateTime.now(clock))) {
            post.setPublishedAt(null);
        }

        applyTags(post, dto.getTags(), dto.getTagIds());
        Post saved = postRepository.save(post);
        return postMapper.toResponseDto(saved);
    }

    /**
     * 投稿を更新。
     * <p>
     * RBAC制御あり。タグ・公開日制御。存在しない場合は空Optional。
     * </p>
     * 
     * @param id   投稿ID
     * @param dto  更新内容
     * @param user 更新ユーザー
     * @return 更新された投稿詳細DTO（Optional）
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
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

    /**
     * 投稿を削除。
     * <p>
     * RBAC制御あり。存在しない場合は例外。
     * </p>
     * 
     * @param id   投稿ID
     * @param user 削除ユーザー
     * @throws com.example.backend.exception.PostNotFoundException 存在しない場合
     * @throws com.example.backend.exception.AccessDeniedException 権限不足
     */
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
