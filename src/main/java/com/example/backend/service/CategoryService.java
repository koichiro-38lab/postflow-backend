package com.example.backend.service;

import com.example.backend.dto.category.CategoryRequestDto;
import com.example.backend.dto.category.CategoryResponseDto;
import com.example.backend.dto.category.CategoryMapper;
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

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryPolicy categoryPolicy;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponseDto> findByIdWithAccessControl(Long id, User user) {
        categoryPolicy.checkRead(user.getRole(), null, null, user.getId());
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponseDto);
    }

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
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponseDto(saved);
    }

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
}