package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BaseException {
    public InvalidRefreshTokenException(String message) {
        super(message, "error.token.refresh.invalid", HttpStatus.UNAUTHORIZED);
    }
}
