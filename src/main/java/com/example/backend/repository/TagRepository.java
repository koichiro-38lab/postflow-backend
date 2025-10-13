package com.example.backend.repository;

import com.example.backend.entity.Tag;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    List<Tag> findBySlugIn(Collection<String> slugs);

    // 公開API用: 公開投稿に紐づくタグ一覧を取得
    @Query("SELECT DISTINCT t FROM Post p JOIN p.tags t WHERE p.status = 'PUBLISHED' AND p.publishedAt <= :now ORDER BY t.name")
    List<Tag> findPublicTags(@Param("now") LocalDateTime now);
}
