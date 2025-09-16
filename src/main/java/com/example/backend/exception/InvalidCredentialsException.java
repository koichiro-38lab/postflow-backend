package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException(String message) {
        super(message, "error.credentials.invalid", HttpStatus.UNAUTHORIZED);
    }
}
