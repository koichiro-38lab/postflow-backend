package com.example.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * タグが投稿などで使用中のため削除できない場合の例外。
 */
public class TagInUseException extends BaseException {
    public TagInUseException(Long tagId) {
        super("Tag is currently in use: " + tagId, "error.tag.in_use", HttpStatus.CONFLICT);
    }
}
