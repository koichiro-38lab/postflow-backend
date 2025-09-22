package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class CategoryInUseException extends BaseException {
    public CategoryInUseException(Long categoryId) {
        super("Category is in use and cannot be deleted: " + categoryId, "error.category.in_use", HttpStatus.CONFLICT);
    }
}