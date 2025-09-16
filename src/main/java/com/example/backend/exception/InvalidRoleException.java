package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidRoleException extends BaseException {
    public InvalidRoleException(String message) {
        super(message, "error.role.invalid", HttpStatus.BAD_REQUEST);
    }
}
