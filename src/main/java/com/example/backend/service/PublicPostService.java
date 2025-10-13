package com.example.backend.service;

import com.example.backend.dto.post.PostMapper;
import com.example.backend.dto.post.PostPublicDetailResponseDto;
import com.example.backend.dto.post.PostPublicResponseDto;
import com.example.backend.entity.Post;
import com.example.backend.exception.PostNotFoundException;
import com.example.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 公開API用の投稿サービス
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final Clock clock;

    /**
     * 公開済み投稿の一覧を取得
     * 
     * @param pageable     ページング情報
     * @param tagSlug      タグスラッグ（フィルタ用、null可）
     * @param categorySlug カテゴリスラッグ（フィルタ用、null可）
     * @return 公開投稿のページ
     */
    public Page<PostPublicResponseDto> getPublicPosts(Pageable pageable, String tagSlug, String categorySlug) {
        LocalDateTime now = LocalDateTime.now(clock);

        Specification<Post> spec = (root, query, cb) -> cb.equal(root.get("status"), Post.Status.PUBLISHED);
        spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("publishedAt"), now));

        if (tagSlug != null && !tagSlug.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("tags").get("slug"), tagSlug));
        }

        if (categorySlug != null && !categorySlug.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("category").get("slug"), categorySlug));
        }

        Page<Post> posts = postRepository.findAll(spec, pageable);

        return posts.map(postMapper::toPublicResponseDto);
    }

    /**
     * スラッグによる公開済み投稿の詳細を取得
     * 
     * @param slug 投稿スラッグ
     * @return 投稿詳細（SEO/OGP情報含む）
     * @throws PostNotFoundException 投稿が見つからない、または未公開の場合
     */
    public PostPublicDetailResponseDto getPublicPostBySlug(String slug) {
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = postRepository.findBySlugAndStatusAndPublishedAtBefore(slug, Post.Status.PUBLISHED, now)
                .orElseThrow(() -> new PostNotFoundException("Post not found or not published: " + slug));
        return postMapper.toPublicDetailResponseDto(post);
    }
}
