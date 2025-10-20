package com.example.backend.service;

import com.example.backend.dto.category.CategoryRequestDto;
import com.example.backend.dto.category.CategoryResponseDto;
import com.example.backend.dto.category.CategoryMapper;
import com.example.backend.dto.category.CategoryReorderRequestDto;
import com.example.backend.entity.Category;
import com.example.backend.entity.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.security.CategoryPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * カテゴリ管理サービス。
 * <p>
 * カテゴリの検索・作成・更新・削除・並び順更新を提供。全操作でRBAC・参照整合性を考慮。
 * <ul>
 * <li>一覧: 親子・sort_order順、投稿数付き取得も可</li>
 * <li>作成: 親カテゴリ指定可、順序自動設定</li>
 * <li>更新: 親カテゴリ・内容変更、存在しない場合は例外</li>
 * <li>削除: 投稿参照時は例外(CategoryInUseException)</li>
 * <li>並び順: 複数カテゴリのsort_order一括更新</li>
 * </ul>
 * 
 * @see com.example.backend.repository.CategoryRepository
 * @see com.example.backend.security.CategoryPolicy
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryPolicy categoryPolicy;
    private final PostRepository postRepository;

    /**
     * 全カテゴリを親子関係・sort_order順で取得。
     * <p>
     * RBAC制御あり。
     * </p>
     * 
     * @return カテゴリ一覧DTO
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> findAll() {
        return categoryRepository.findAllOrderByParentAndSort().stream()
                .map(categoryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 全カテゴリを親子関係・sort_order順で取得（投稿数付き）。
     * <p>
     * 各カテゴリに紐づく投稿数も付与。
     * </p>
     * 
     * @return カテゴリ一覧DTO（投稿数付き）
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> findAllWithPostCount() {
        return categoryRepository.findAllOrderByParentAndSort().stream()
                .map(category -> {
                    long postCount = postRepository.countByCategoryId(category.getId());
                    return categoryMapper.toResponseDtoWithPostCount(category, (int) postCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * ID指定でカテゴリを取得（アクセス制御付き）。
     * <p>
     * RBAC制御あり。存在しない場合は空Optional。
     * </p>
     * 
     * @param id   カテゴリID
     * @param user 取得ユーザー
     * @return カテゴリー情報（Optional）
     */
    @Transactional(readOnly = true)
    public Optional<CategoryResponseDto> findByIdWithAccessControl(Long id, User user) {
        categoryPolicy.checkRead(user.getRole(), null, null, user.getId());
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponseDto);
    }

    /**
     * カテゴリを新規作成。
     * <p>
     * RBAC制御あり。親カテゴリ指定可。順序は同一親の末尾。
     * </p>
     * 
     * @param dto  カテゴリ作成リクエスト
     * @param user 作成ユーザー
     * @return 作成されたカテゴリ情報
     * @throws com.example.backend.exception.CategoryNotFoundException 親カテゴリが存在しない場合
     */
    @Transactional
    public CategoryResponseDto create(CategoryRequestDto dto, User user) {
        categoryPolicy.checkCreate(user.getRole(), null, null, user.getId());
        Category category = new Category();
        categoryMapper.applyToEntity(category, dto);
        if (dto.parentId() != null) {
            Category parent = categoryRepository.findById(dto.parentId())
                    .orElseThrow(() -> new com.example.backend.exception.CategoryNotFoundException(dto.parentId()));
            category.setParent(parent);
        }

        // 新規カテゴリの順序を設定（同一親の最後に追加）
        Integer maxSortOrder = categoryRepository.findMaxSortOrderByParent(dto.parentId());
        category.setSortOrder(maxSortOrder != null ? maxSortOrder + 1 : 0);

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponseDto(saved);
    }

    /**
     * カテゴリを更新。
     * <p>
     * RBAC制御あり。親カテゴリ・内容変更可。存在しない場合は空Optional。
     * </p>
     * 
     * @param id   カテゴリID
     * @param dto  更新内容
     * @param user 更新ユーザー
     * @return 更新されたカテゴリ情報（Optional）
     * @throws com.example.backend.exception.CategoryNotFoundException 親カテゴリが存在しない場合
     */
    @Transactional
    public Optional<CategoryResponseDto> update(Long id, CategoryRequestDto dto, User user) {
        categoryPolicy.checkUpdate(user.getRole(), null, null, user.getId());
        return categoryRepository.findById(id).map(category -> {
            categoryMapper.applyToEntity(category, dto);
            if (dto.parentId() != null) {
                Category parent = categoryRepository.findById(dto.parentId())
                        .orElseThrow(() -> new com.example.backend.exception.CategoryNotFoundException(dto.parentId()));
                category.setParent(parent);
            } else {
                category.setParent(null);
            }
            return categoryMapper.toResponseDto(category);
        });
    }

    /**
     * カテゴリを削除。
     * <p>
     * RBAC制御あり。投稿で参照されている場合はCategoryInUseException。
     * </p>
     * 
     * @param id   カテゴリID
     * @param user 削除ユーザー
     * @throws com.example.backend.exception.CategoryNotFoundException 存在しない場合
     * @throws com.example.backend.exception.CategoryInUseException    投稿参照時
     */
    @Transactional
    public void delete(Long id, User user) {
        categoryPolicy.checkDelete(user.getRole(), null, null, user.getId());
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new com.example.backend.exception.CategoryNotFoundException(id));
        if (postRepository.existsByCategoryId(id)) {
            throw new com.example.backend.exception.CategoryInUseException(id);
        }
        categoryRepository.delete(category);
    }

    /**
     * カテゴリの並び順を一括更新。
     * <p>
     * RBAC制御あり。複数カテゴリのsort_orderを一括で更新。
     * </p>
     * 
     * @param reorderRequests 並び順更新リクエスト一覧
     * @param user            更新ユーザー
     * @throws com.example.backend.exception.CategoryNotFoundException 存在しないカテゴリ指定時
     */
    @Transactional
    public void reorderCategories(List<CategoryReorderRequestDto> reorderRequests, User user) {
        categoryPolicy.checkUpdate(user.getRole(), null, null, user.getId());

        for (CategoryReorderRequestDto request : reorderRequests) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(
                            () -> new com.example.backend.exception.CategoryNotFoundException(request.getCategoryId()));
            category.setSortOrder(request.getNewSortOrder());
            categoryRepository.save(category);
        }
    }
}