package com.example.backend.service;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.category.CategoryRequestDto;
import com.example.backend.dto.category.CategoryResponseDto;
import com.example.backend.entity.User;
import com.example.backend.exception.CategoryNotFoundException;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({ TestDataConfig.class, TestClockConfig.class })
@ActiveProfiles("test")
@Transactional
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        postRepository.flush();
        categoryRepository.flush();
    }

    // 存在しない親カテゴリIDで作成しようとした場合のテスト
    @Test
    void createCategory_withNonExistentParentId_shouldThrowException() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        CategoryRequestDto dto = new CategoryRequestDto(
                "Child Category",
                "child-category",
                999999L // 存在しない親カテゴリID
        );

        assertThatThrownBy(() -> categoryService.create(dto, admin))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // 存在しない親カテゴリIDで更新しようとした場合のテスト
    @Test
    void updateCategory_withNonExistentParentId_shouldThrowException() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // まずカテゴリを作成
        CategoryRequestDto createDto = new CategoryRequestDto(
                "Original Category",
                "original-category",
                null);
        CategoryResponseDto created = categoryService.create(createDto, admin);

        // 存在しない親カテゴリIDで更新
        CategoryRequestDto updateDto = new CategoryRequestDto(
                "Updated Category",
                "updated-category",
                999999L // 存在しない親カテゴリID
        );

        assertThatThrownBy(() -> categoryService.update(created.id(), updateDto, admin))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // 存在しないIDで更新しようとした場合のテスト
    @Test
    void updateCategory_withNonExistentId_shouldReturnEmpty() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        Long nonExistentId = 999999L;
        CategoryRequestDto updateDto = new CategoryRequestDto(
                "Updated Category",
                "updated-category",
                null);

        assertThat(categoryService.update(nonExistentId, updateDto, admin)).isEmpty();
    }

    // 存在しないIDで削除しようとした場合のテスト
    @Test
    void deleteCategory_withNonExistentId_shouldThrowException() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        Long nonExistentId = 999999L;

        assertThatThrownBy(() -> categoryService.delete(nonExistentId, admin))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // 存在しないIDで取得しようとした場合のテスト
    @Test
    void findById_withNonExistentId_shouldReturnEmpty() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        Long nonExistentId = 999999L;

        assertThat(categoryService.findByIdWithAccessControl(nonExistentId, admin)).isEmpty();
    }

    // 正常なカテゴリ作成のテスト
    @Test
    void createCategory_withValidData_shouldCreateCategory() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        CategoryRequestDto dto = new CategoryRequestDto(
                "Valid Category",
                "valid-category",
                null);

        CategoryResponseDto result = categoryService.create(dto, admin);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Valid Category");
        assertThat(result.slug()).isEqualTo("valid-category");
    }

    // 正常な親カテゴリ付きカテゴリ作成のテスト
    @Test
    void createCategory_withParentId_shouldCreateCategoryWithParent() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // 親カテゴリを作成
        CategoryRequestDto parentDto = new CategoryRequestDto(
                "Parent Category",
                "parent-category",
                null);
        CategoryResponseDto parent = categoryService.create(parentDto, admin);

        // 子カテゴリを作成
        CategoryRequestDto childDto = new CategoryRequestDto(
                "Child Category",
                "child-category",
                parent.id());
        CategoryResponseDto child = categoryService.create(childDto, admin);

        assertThat(child).isNotNull();
    }

    // 正常なカテゴリ更新のテスト
    @Test
    void updateCategory_withValidData_shouldUpdateCategory() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // まずカテゴリを作成
        CategoryRequestDto createDto = new CategoryRequestDto(
                "Original Category",
                "original-category",
                null);
        CategoryResponseDto created = categoryService.create(createDto, admin);

        // 更新
        CategoryRequestDto updateDto = new CategoryRequestDto(
                "Updated Category",
                "updated-category",
                null);
        var result = categoryService.update(created.id(), updateDto, admin);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Updated Category");
        assertThat(result.get().slug()).isEqualTo("updated-category");
    }

    // 正常なカテゴリ削除のテスト
    @Test
    void deleteCategory_withValidId_shouldDeleteCategory() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // まずカテゴリを作成
        CategoryRequestDto createDto = new CategoryRequestDto(
                "Category to Delete",
                "category-to-delete",
                null);
        CategoryResponseDto created = categoryService.create(createDto, admin);

        // 削除
        categoryService.delete(created.id(), admin);

        // 削除確認
        assertThat(categoryService.findByIdWithAccessControl(created.id(), admin)).isEmpty();
    }

    // 投稿数付きカテゴリ取得のテスト
    @Test
    void findAllWithPostCount_shouldReturnCategoriesWithPostCount() {
        var result = categoryService.findAllWithPostCount();

        assertThat(result).isNotNull();
        // 各カテゴリに投稿数が付与されていることを確認
        result.forEach(category -> {
            assertThat(category.postCount()).isNotNull();
            assertThat(category.postCount()).isGreaterThanOrEqualTo(0);
        });
    }
}
