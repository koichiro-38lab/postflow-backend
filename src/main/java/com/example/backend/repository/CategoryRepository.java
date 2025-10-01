package com.example.backend.repository;

import com.example.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c ORDER BY c.parent.id ASC NULLS FIRST, c.sortOrder ASC")
    List<Category> findAllOrderByParentAndSort();

    @Query("SELECT MAX(c.sortOrder) FROM Category c WHERE c.parent.id = :parentId OR (c.parent IS NULL AND :parentId IS NULL)")
    Integer findMaxSortOrderByParent(Long parentId);
}
