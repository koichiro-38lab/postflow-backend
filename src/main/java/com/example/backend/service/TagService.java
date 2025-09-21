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
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-z0-9-]{1,255}$");

    @Transactional(readOnly = true)
    public List<TagResponseDto> findAll() {
        return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(tagMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public TagResponseDto create(TagRequestDto request, com.example.backend.entity.User.Role role) {
        tagPolicy.checkCreate(role);
        String normalizedName = normalizeName(request.getName());
        String normalizedSlug = normalizeSlug(request.getSlug());
        Tag tag = Tag.builder()
                .name(normalizedName)
                .slug(normalizedSlug)
                .build();
        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponseDto(saved);
    }

    @Transactional
    public TagResponseDto update(Long id, TagRequestDto request, com.example.backend.entity.User.Role role) {
        tagPolicy.checkUpdate(role);
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
        String normalizedName = normalizeName(request.getName());
        String normalizedSlug = normalizeSlug(request.getSlug());
        tag.setName(normalizedName);
        tag.setSlug(normalizedSlug);
        return tagMapper.toResponseDto(tag);
    }

    @Transactional
    public void delete(Long id, com.example.backend.entity.User.Role role) {
        tagPolicy.checkDelete(role);
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
        tagRepository.delete(tag);
    }

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

    private String normalizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tag name must not be null");
        }
        String trimmed = value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (!TAG_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Tag name must match pattern [a-z0-9-]{1,255}");
        }
        return normalized;
    }

    public String normalizeSlug(String value) {
        return normalizeName(value);
    }
}
