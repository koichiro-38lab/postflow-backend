package com.example.backend.repository;

import com.example.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    Optional<Post> findBySlug(String slug);

    boolean existsByCoverMediaId(Long mediaId);

    // 指定したタグIDを含む投稿が存在するか判定（タグ削除前チェック用）
    boolean existsByTags_Id(Long tagId);
}
