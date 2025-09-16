package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends BaseException {
    public AccessDeniedException(String message) {
        super(message, "error.access.denied", HttpStatus.FORBIDDEN);
    }
}