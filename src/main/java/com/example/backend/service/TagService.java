package com.example.backend.service;

import com.example.backend.dto.tag.TagRequestDto;
import com.example.backend.dto.tag.TagResponseDto;
import com.example.backend.entity.Tag;
import com.example.backend.exception.TagNotFoundException;
import com.example.backend.repository.TagRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final com.example.backend.security.TagPolicy tagPolicy;
    private final com.example.backend.dto.tag.TagMapper tagMapper;
    private static final Pattern TAG_PATTERN = Pattern
            .compile(
                    "^[\\w\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\uAC00-\\uD7AF\\uFF00-\\uFFEF\\s\\p{Punct}/]{1,255}$");
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9-]{1,255}$");

    /**
     * 全てのタグを取得
     *
     * @return タグのリスト（名前順）
     */
    @Transactional(readOnly = true)
    public List<TagResponseDto> findAll() {
        return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(tagMapper::toResponseDto)
                .toList();
    }

    /**
     * IDでタグを取得
     * 
     * @param id タグID
     * @return タグ
     * @throws TagNotFoundException タグが存在しない場合
     */
    @Transactional
    public TagResponseDto create(TagRequestDto request, com.example.backend.entity.User.Role role) {
        tagPolicy.checkCreate(role, null, null, null);
        String normalizedName = normalizeName(request.getName());
        String normalizedSlug = normalizeSlug(request.getSlug());
        Tag tag = Tag.builder()
                .name(normalizedName)
                .slug(normalizedSlug)
                .build();
        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponseDto(saved);
    }

    /**
     * IDでタグを更新
     * 
     * @param id タグID
     * @return タグ
     * @throws TagNotFoundException タグが存在しない場合
     */
    @Transactional
    public TagResponseDto update(Long id, TagRequestDto request, com.example.backend.entity.User.Role role) {
        tagPolicy.checkUpdate(role, null, null, null);
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
        String normalizedName = normalizeName(request.getName());
        String normalizedSlug = normalizeSlug(request.getSlug());
        tag.setName(normalizedName);
        tag.setSlug(normalizedSlug);
        return tagMapper.toResponseDto(tag);
    }

    /**
     * IDでタグを削除
     * 
     * @param id タグID
     * @throws TagNotFoundException タグが存在しない場合
     */
    @Transactional
    public void delete(Long id, com.example.backend.entity.User.Role role) {
        tagPolicy.checkDelete(role, null, null, null);
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
        tagRepository.delete(tag);
    }

    /**
     * スラッグでタグを取得
     * 
     * @param slugs タグのスラッグリスト
     * @return タグのリスト（入力順）
     * @throws IllegalArgumentException 存在しないタグが含まれる場合
     */
    @Transactional(readOnly = true)
    public List<Tag> findAllBySlugs(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return List.of();
        }
        List<String> normalized = slugs.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeName)
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>(normalized);
        List<Tag> tags = tagRepository.findBySlugIn(unique);
        Map<String, Tag> tagMap = tags.stream()
                .collect(Collectors.toMap(Tag::getSlug, tag -> tag, (a, b) -> a, LinkedHashMap::new));
        List<String> missing = unique.stream()
                .filter(slug -> !tagMap.containsKey(slug))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Tag not found: " + String.join(", ", missing));
        }
        return unique.stream()
                .map(tagMap::get)
                .toList();
    }

    /**
     * IDでタグの取得
     * 
     * @param ids タグIDのリスト
     * @return タグのリスト（入力順）
     * @throws IllegalArgumentException 存在しないタグが含まれる場合
     */
    @Transactional(readOnly = true)
    public List<Tag> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Tag> tags = tagRepository.findAllById(ids);
        Set<Long> foundIds = tags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());
        List<Long> missing = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Tag not found: " + missing);
        }
        return tags;
    }

    /**
     * タグ名の正規化
     * 
     * @param value タグ名
     * @return 正規化されたタグ名
     * @throws IllegalArgumentException 無効なタグ名の場合
     */
    private String normalizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tag name must not be null");
        }
        String trimmed = value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        // タグ名は小文字化しない（日本語やスラッシュなどをそのまま保持）
        if (!TAG_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Tag name must match pattern [\\w\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\uAC00-\\uD7AF\\uFF00-\\uFFEF\\s\\p{Punct}/]{1,255}, but got: '"
                            + trimmed + "'");
        }
        return trimmed;
    }

    /**
     * スラッグの正規化
     *
     * @param value スラッグ
     * @return 正規化されたスラッグ
     * @throws IllegalArgumentException 無効なスラッグの場合
     */
    public String normalizeSlug(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tag slug must not be null");
        }
        String trimmed = value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Tag slug must not be blank");
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Tag slug must match pattern [a-z0-9-]{1,255}");
        }
        return normalized;
    }
}
