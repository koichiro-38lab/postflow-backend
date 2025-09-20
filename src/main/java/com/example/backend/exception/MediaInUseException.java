package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class MediaInUseException extends BaseException {
    public MediaInUseException(String errorCode) {
        super("Media resource is currently in use", errorCode, HttpStatus.CONFLICT);
    }
}
