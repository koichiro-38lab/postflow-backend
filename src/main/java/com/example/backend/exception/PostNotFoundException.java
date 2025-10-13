package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class PostNotFoundException extends BaseException {
    public PostNotFoundException(Long postId) {
        super("Post not found: " + postId, "error.post.not_found", HttpStatus.NOT_FOUND);
    }

    public PostNotFoundException(String message) {
        super(message, "error.post.not_found", HttpStatus.NOT_FOUND);
    }
}
