package com.example.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * アプリケーション例外の基底クラス
 */
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    protected BaseException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}