package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaPolicyTest {

    private UserRepository userRepository;
    private MediaPolicy mediaPolicy;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mediaPolicy = new MediaPolicy(userRepository);
    }

    // AdminとEditorは全てのMediaを読むことができる
    @Test
    void adminCanRead() {
        assertDoesNotThrow(() -> mediaPolicy.checkRead(User.Role.ADMIN, null, null, 1L));
    }

    // Editorも全てのMediaを読むことができる
    @Test
    void editorCanRead() {
        assertDoesNotThrow(() -> mediaPolicy.checkRead(User.Role.EDITOR, null, null, 1L));
    }

    // Authorは自分のMediaのみ読むことができる
    @Test
    void authorCanReadOwnResource() {
        assertDoesNotThrow(() -> mediaPolicy.checkRead(User.Role.AUTHOR, 2L, null, 2L));
    }

    // Authorは他人のMediaを読むことはできない
    @Test
    void authorCannotReadOthers() {
        assertThrows(AccessDeniedException.class, () -> mediaPolicy.checkRead(User.Role.AUTHOR, 2L, null, 3L));
    }

    // AuthorはAvatarとして使われているMediaを読むことができる
    @Test
    void readForMedia_allowsIfAvatarUsed() {
        when(userRepository.existsByAvatarMediaId(10L)).thenReturn(true);
        assertDoesNotThrow(() -> mediaPolicy.checkReadForMedia(User.Role.AUTHOR, 10L, null, 3L));
        verify(userRepository).existsByAvatarMediaId(10L);
    }

    // AuthorはAvatarとして使われていないMediaを読むことはできない
    @Test
    void readForMedia_deniesIfNotAvatarAndNotOwner() {
        when(userRepository.existsByAvatarMediaId(11L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> mediaPolicy.checkReadForMedia(User.Role.AUTHOR, 11L, null, 3L));
    }

    // AuthorはAvatarとして使われていないMediaでも自分のMediaなら読むことができる
    @Test
    void create_authorOwnAllowed() {
        assertDoesNotThrow(() -> mediaPolicy.checkCreate(User.Role.AUTHOR, 5L, null, 5L));
    }

    // Authorは他人のMediaを作成することはできない
    @Test
    void create_authorNotOwnerDenied() {
        assertThrows(AccessDeniedException.class, () -> mediaPolicy.checkCreate(User.Role.AUTHOR, 5L, null, 6L));
    }

    // AdminとEditorは全てのMediaを作成できる
    @Test
    void delete_delegatesToRead() {
        assertDoesNotThrow(() -> mediaPolicy.checkDelete(User.Role.ADMIN, 1L, null, 2L));
    }
}
