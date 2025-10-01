package com.example.backend.controller.admin;

import com.example.backend.dto.category.CategoryRequestDto;
import com.example.backend.dto.category.CategoryResponseDto;
import com.example.backend.dto.category.CategoryReorderRequestDto;
import com.example.backend.service.CategoryService;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping
    public List<CategoryResponseDto> getCategories(@AuthenticationPrincipal Jwt jwt) {
        return categoryService.findAllWithPostCount();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return categoryService.findByIdWithAccessControl(id, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDto> create(
            @RequestBody @Valid CategoryRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        CategoryResponseDto created = categoryService.create(dto, currentUser);
        return ResponseEntity.created(URI.create("/api/admin/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid CategoryRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        return categoryService.update(id, dto, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        categoryService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderCategories(
            @RequestBody @Valid List<CategoryReorderRequestDto> reorderRequests,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getCurrentUser(jwt);
        categoryService.reorderCategories(reorderRequests, currentUser);
        return ResponseEntity.ok().build();
    }
}