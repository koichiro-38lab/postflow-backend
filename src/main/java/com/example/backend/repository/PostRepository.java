package com.example.backend.repository;

import com.example.backend.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    Optional<Post> findBySlug(String slug);

    boolean existsByCoverMediaId(Long mediaId);

    // 指定したタグIDを含む投稿が存在するか判定（タグ削除前チェック用）
    boolean existsByTags_Id(Long tagId);

    // 指定したタグIDを含む投稿数をカウント
    long countByTags_Id(Long tagId);

    // 指定したカテゴリIDを含む投稿が存在するか判定（カテゴリ削除前チェック用）
    boolean existsByCategoryId(Long categoryId);

    // 指定したカテゴリIDの投稿数をカウント
    long countByCategoryId(Long categoryId);

    // 公開API用: 公開済み投稿の一覧取得（公開日時降順）
    Page<Post> findByStatusAndPublishedAtBeforeOrderByPublishedAtDesc(
            Post.Status status, LocalDateTime now, Pageable pageable);

    // 公開API用: タグフィルタ付き公開済み投稿の一覧取得
    Page<Post> findByStatusAndPublishedAtBeforeAndTags_SlugOrderByPublishedAtDesc(
            Post.Status status, LocalDateTime now, String tagSlug, Pageable pageable);

    // 公開API用: カテゴリフィルタ付き公開済み投稿の一覧取得
    Page<Post> findByStatusAndPublishedAtBeforeAndCategory_SlugOrderByPublishedAtDesc(
            Post.Status status, LocalDateTime now, String categorySlug, Pageable pageable);

    // 公開API用: スラッグによる公開済み投稿の詳細取得
    Optional<Post> findBySlugAndStatusAndPublishedAtBefore(
            String slug, Post.Status status, LocalDateTime now);
}
