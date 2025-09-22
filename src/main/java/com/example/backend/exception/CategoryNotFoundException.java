package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends BaseException {
    public CategoryNotFoundException(Long categoryId) {
        super("Category not found: " + categoryId, "error.category.not_found", HttpStatus.NOT_FOUND);
    }
}