package com.example.backend.service;

import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;
import com.example.backend.dto.tag.TagRequestDto;
import com.example.backend.dto.tag.TagResponseDto;
import com.example.backend.entity.Tag;
import com.example.backend.entity.User;
import com.example.backend.exception.TagNotFoundException;
import com.example.backend.repository.TagRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({ TestDataConfig.class, TestClockConfig.class })
@ActiveProfiles("test")
@Transactional
class TagServiceTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void tearDown() {
        tagRepository.deleteAll();
        tagRepository.flush();
    }

    // nullのタグ名で作成しようとした場合のテスト
    @Test
    void createTag_withNullName_shouldThrowException() {
        TagRequestDto dto = TagRequestDto.builder()
                .name(null)
                .slug("test-slug")
                .build();

        assertThatThrownBy(() -> tagService.create(dto, User.Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag name must not be null");
    }

    // 空白のタグ名で作成しようとした場合のテスト
    @Test
    void createTag_withBlankName_shouldThrowException() {
        TagRequestDto dto = TagRequestDto.builder()
                .name("   ")
                .slug("test-slug")
                .build();

        assertThatThrownBy(() -> tagService.create(dto, User.Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag name must not be blank");
    }

    // nullのスラッグで作成しようとした場合のテスト
    @Test
    void createTag_withNullSlug_shouldThrowException() {
        TagRequestDto dto = TagRequestDto.builder()
                .name("Test Tag")
                .slug(null)
                .build();

        assertThatThrownBy(() -> tagService.create(dto, User.Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag slug must not be null");
    }

    // 空白のスラッグで作成しようとした場合のテスト
    @Test
    void createTag_withBlankSlug_shouldThrowException() {
        TagRequestDto dto = TagRequestDto.builder()
                .name("Test Tag")
                .slug("   ")
                .build();

        assertThatThrownBy(() -> tagService.create(dto, User.Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag slug must not be blank");
    }

    // 不正なスラッグパターンで作成しようとした場合のテスト
    @Test
    void createTag_withInvalidSlugPattern_shouldThrowException() {
        TagRequestDto dto = TagRequestDto.builder()
                .name("Test Tag")
                .slug("Invalid_Slug!")
                .build();

        assertThatThrownBy(() -> tagService.create(dto, User.Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag slug must match pattern [a-z0-9-]{1,255}");
    }

    // 存在しないIDで更新しようとした場合のテスト
    @Test
    void updateTag_withNonExistentId_shouldThrowException() {
        Long nonExistentId = 999999L;
        TagRequestDto dto = TagRequestDto.builder()
                .name("Updated Tag")
                .slug("updated-slug")
                .build();

        assertThatThrownBy(() -> tagService.update(nonExistentId, dto, User.Role.ADMIN))
                .isInstanceOf(TagNotFoundException.class);
    }

    // 存在しないIDで削除しようとした場合のテスト
    @Test
    void deleteTag_withNonExistentId_shouldThrowException() {
        Long nonExistentId = 999999L;

        assertThatThrownBy(() -> tagService.delete(nonExistentId, User.Role.ADMIN))
                .isInstanceOf(TagNotFoundException.class);
    }

    // nullのスラッグリストで検索した場合のテスト
    @Test
    void findAllBySlugs_withNullList_shouldReturnEmptyList() {
        List<Tag> result = tagService.findAllBySlugs(null);
        assertThat(result).isEmpty();
    }

    // 空のスラッグリストで検索した場合のテスト
    @Test
    void findAllBySlugs_withEmptyList_shouldReturnEmptyList() {
        List<Tag> result = tagService.findAllBySlugs(List.of());
        assertThat(result).isEmpty();
    }

    // 空白のみのスラッグリストで検索した場合のテスト
    @Test
    void findAllBySlugs_withBlankSlugs_shouldReturnEmptyList() {
        List<Tag> result = tagService.findAllBySlugs(List.of("  ", "", "   "));
        assertThat(result).isEmpty();
    }

    // 存在しないスラッグで検索した場合のテスト
    @Test
    void findAllBySlugs_withNonExistentSlug_shouldThrowException() {
        assertThatThrownBy(() -> tagService.findAllBySlugs(List.of("non-existent-slug")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag not found:");
    }

    // nullのIDリストで検索した場合のテスト
    @Test
    void findAllByIds_withNullList_shouldReturnEmptyList() {
        List<Tag> result = tagService.findAllByIds(null);
        assertThat(result).isEmpty();
    }

    // 空のIDリストで検索した場合のテスト
    @Test
    void findAllByIds_withEmptyList_shouldReturnEmptyList() {
        List<Tag> result = tagService.findAllByIds(List.of());
        assertThat(result).isEmpty();
    }

    // 存在しないIDで検索した場合のテスト
    @Test
    void findAllByIds_withNonExistentId_shouldThrowException() {
        assertThatThrownBy(() -> tagService.findAllByIds(List.of(999999L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag not found:");
    }

    // 正常なタグ作成のテスト
    @Test
    void createTag_withValidData_shouldCreateTag() {
        TagRequestDto dto = TagRequestDto.builder()
                .name("Valid Tag")
                .slug("valid-tag")
                .build();

        TagResponseDto result = tagService.create(dto, User.Role.ADMIN);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Valid Tag");
        assertThat(result.getSlug()).isEqualTo("valid-tag");
    }

    // 正常なタグ更新のテスト
    @Test
    void updateTag_withValidData_shouldUpdateTag() {
        // まずタグを作成
        TagRequestDto createDto = TagRequestDto.builder()
                .name("Original Tag")
                .slug("original-tag")
                .build();
        TagResponseDto created = tagService.create(createDto, User.Role.ADMIN);

        // 更新
        TagRequestDto updateDto = TagRequestDto.builder()
                .name("Updated Tag")
                .slug("updated-tag")
                .build();
        TagResponseDto result = tagService.update(created.getId(), updateDto, User.Role.ADMIN);

        assertThat(result.getName()).isEqualTo("Updated Tag");
        assertThat(result.getSlug()).isEqualTo("updated-tag");
    }

    // 正常なタグ削除のテスト
    @Test
    void deleteTag_withValidId_shouldDeleteTag() {
        // まずタグを作成
        TagRequestDto createDto = TagRequestDto.builder()
                .name("Tag to Delete")
                .slug("tag-to-delete")
                .build();
        TagResponseDto created = tagService.create(createDto, User.Role.ADMIN);

        // 削除
        tagService.delete(created.getId(), User.Role.ADMIN);

        // 削除確認
        assertThatThrownBy(() -> tagService.update(created.getId(), createDto, User.Role.ADMIN))
                .isInstanceOf(TagNotFoundException.class);
    }
}
