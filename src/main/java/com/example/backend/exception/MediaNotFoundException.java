package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class MediaNotFoundException extends BaseException {
    public MediaNotFoundException(Long id) {
        super("Media not found: " + id, "error.media.notFound", HttpStatus.NOT_FOUND);
    }
}
