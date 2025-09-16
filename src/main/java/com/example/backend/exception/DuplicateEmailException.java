package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BaseException {
    public DuplicateEmailException(String message) {
        super(message, "error.email.duplicate", HttpStatus.CONFLICT);
    }
}