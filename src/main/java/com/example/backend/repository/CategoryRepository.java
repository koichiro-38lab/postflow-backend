package com.example.backend.repository;

import com.example.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c ORDER BY c.parent.id ASC NULLS FIRST, c.sortOrder ASC")
    List<Category> findAllOrderByParentAndSort();

    @Query("SELECT MAX(c.sortOrder) FROM Category c WHERE c.parent.id = :parentId OR (c.parent IS NULL AND :parentId IS NULL)")
    Integer findMaxSortOrderByParent(Long parentId);

    // 公開API用: 公開投稿に紐づくカテゴリ一覧を取得
    @Query("SELECT DISTINCT p.category FROM Post p WHERE p.status = 'PUBLISHED' AND p.publishedAt <= :now AND p.category IS NOT NULL ORDER BY p.category.name")
    List<Category> findPublicCategories(@Param("now") LocalDateTime now);
}
