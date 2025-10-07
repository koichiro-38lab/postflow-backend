package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostPolicyTest {

    private final PostPolicy postPolicy = new PostPolicy();

    // AdminとEditorは全ての操作が可能
    @Test
    void adminAndEditorCanReadCreateUpdateDelete() {
        assertDoesNotThrow(() -> postPolicy.checkRead(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> postPolicy.checkRead(User.Role.EDITOR, 1L, null, 2L));

        assertDoesNotThrow(() -> postPolicy.checkCreate(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> postPolicy.checkCreate(User.Role.EDITOR, 1L, null, 2L));

        assertDoesNotThrow(() -> postPolicy.checkUpdate(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> postPolicy.checkUpdate(User.Role.EDITOR, 1L, null, 2L));

        assertDoesNotThrow(() -> postPolicy.checkDelete(User.Role.ADMIN, 1L, null, 2L));
        assertDoesNotThrow(() -> postPolicy.checkDelete(User.Role.EDITOR, 1L, null, 2L));
    }

    // Authorは自分のPostに対してのみ操作が可能
    @Test
    void authorCanOperateOnOwnResource() {
        // resourceOwnerId == currentUserId
        assertDoesNotThrow(() -> postPolicy.checkRead(User.Role.AUTHOR, 5L, null, 5L));
        assertDoesNotThrow(() -> postPolicy.checkCreate(User.Role.AUTHOR, 5L, null, 5L));
        assertDoesNotThrow(() -> postPolicy.checkUpdate(User.Role.AUTHOR, 5L, 5L, 5L));
        assertDoesNotThrow(() -> postPolicy.checkDelete(User.Role.AUTHOR, 5L, null, 5L));
    }

    // Authorは他人のPostに対しては全て拒否される
    @Test
    void authorCannotAccessOthers() {
        assertThrows(AccessDeniedException.class, () -> postPolicy.checkRead(User.Role.AUTHOR, 6L, null, 7L));
        assertThrows(AccessDeniedException.class, () -> postPolicy.checkCreate(User.Role.AUTHOR, 6L, null, 7L));
        assertThrows(AccessDeniedException.class, () -> postPolicy.checkUpdate(User.Role.AUTHOR, 6L, null, 7L));
        assertThrows(AccessDeniedException.class, () -> postPolicy.checkDelete(User.Role.AUTHOR, 6L, null, 7L));
    }

    // AuthorがactingUserIdを指定して更新しようとした場合、actingUserId != currentUserIdなら拒否される
    @Test
    void authorCannotChangeAuthorWhenActingAsDifferentUser() {
        assertThrows(AccessDeniedException.class, () -> postPolicy.checkUpdate(User.Role.AUTHOR, 8L, 9L, 8L));
    }
}
