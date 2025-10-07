package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryPolicyTest {

    private final CategoryPolicy categoryPolicy = new CategoryPolicy();

    // AdminとEditorは全ての操作が可能
    @Test
    void adminOrEditorCanCreateUpdateDelete() {
        assertDoesNotThrow(() -> categoryPolicy.checkCreate(User.Role.ADMIN, null, null, 1L));
        assertDoesNotThrow(() -> categoryPolicy.checkCreate(User.Role.EDITOR, null, null, 1L));

        assertDoesNotThrow(() -> categoryPolicy.checkUpdate(User.Role.ADMIN, null, null, 1L));
        assertDoesNotThrow(() -> categoryPolicy.checkUpdate(User.Role.EDITOR, null, null, 1L));

        assertDoesNotThrow(() -> categoryPolicy.checkDelete(User.Role.ADMIN, null, null, 1L));
        assertDoesNotThrow(() -> categoryPolicy.checkDelete(User.Role.EDITOR, null, null, 1L));
    }

    // AdminでもEditorでもない場合は拒否される
    @Test
    void nonAdminNonEditorDenied() {
        assertThrows(AccessDeniedException.class, () -> categoryPolicy.checkCreate(User.Role.AUTHOR, null, null, 2L));
        assertThrows(AccessDeniedException.class, () -> categoryPolicy.checkUpdate(User.Role.AUTHOR, null, null, 2L));
        assertThrows(AccessDeniedException.class, () -> categoryPolicy.checkDelete(User.Role.AUTHOR, null, null, 2L));
    }
}
