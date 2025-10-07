package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TagPolicyTest {

    private final TagPolicy tagPolicy = new TagPolicy();

    // AdminとEditorは全ての操作が可能
    @Test
    void adminAndEditorAllowedForAll() {
        assertDoesNotThrow(() -> tagPolicy.checkRead(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> tagPolicy.checkRead(User.Role.EDITOR, 1L, null, 2L));

        assertDoesNotThrow(() -> tagPolicy.checkCreate(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> tagPolicy.checkCreate(User.Role.EDITOR, 1L, null, 2L));

        assertDoesNotThrow(() -> tagPolicy.checkUpdate(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> tagPolicy.checkUpdate(User.Role.EDITOR, 1L, null, 2L));

        assertDoesNotThrow(() -> tagPolicy.checkDelete(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> tagPolicy.checkDelete(User.Role.EDITOR, 1L, null, 2L));
    }

    // Authorは自分のTagに対してのみ操作が可能
    @Test
    void authorAllowedOnlyForOwn() {
        // オーナー
        assertDoesNotThrow(() -> tagPolicy.checkRead(User.Role.AUTHOR, 5L, null, 5L));
        assertDoesNotThrow(() -> tagPolicy.checkCreate(User.Role.AUTHOR, 5L, null, 5L));
        assertDoesNotThrow(() -> tagPolicy.checkUpdate(User.Role.AUTHOR, 5L, null, 5L));
        assertDoesNotThrow(() -> tagPolicy.checkDelete(User.Role.AUTHOR, 5L, null, 5L));

        // 非オーナー
        assertThrows(AccessDeniedException.class, () -> tagPolicy.checkRead(User.Role.AUTHOR, 5L, null, 6L));
        assertThrows(AccessDeniedException.class, () -> tagPolicy.checkCreate(User.Role.AUTHOR, 5L, null, 6L));
        assertThrows(AccessDeniedException.class, () -> tagPolicy.checkUpdate(User.Role.AUTHOR, 5L, null, 6L));
        assertThrows(AccessDeniedException.class, () -> tagPolicy.checkDelete(User.Role.AUTHOR, 5L, null, 6L));
    }
}
