package com.example.backend.entity;

public enum UserStatus {
    ACTIVE, // アクティブなユーザー（通常の状態）
    INACTIVE, // 非アクティブ（自主的に無効化）
    SUSPENDED, // 停止中（管理者により一時停止）
    DELETED // 削除済み（論理削除）
}
