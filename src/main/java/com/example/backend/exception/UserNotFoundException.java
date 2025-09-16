package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException(String message) {
        super(message, "error.user.not_found", HttpStatus.NOT_FOUND);
    }
}