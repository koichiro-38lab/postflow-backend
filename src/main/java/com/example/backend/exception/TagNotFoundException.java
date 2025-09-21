package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class TagNotFoundException extends BaseException {
    public TagNotFoundException(Long tagId) {
        super("Tag not found: " + tagId, "error.tag.not_found", HttpStatus.NOT_FOUND);
    }
}
