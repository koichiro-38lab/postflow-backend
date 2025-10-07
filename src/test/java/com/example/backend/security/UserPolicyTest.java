package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPolicyTest {

    private final UserPolicy userPolicy = new UserPolicy();

    // ユーザー作成のヘルパー
    private User user(Long id, User.Role role) {
        return User.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .passwordHash("pw")
                .role(role)
                .build();
    }

    // 自分のプロフィール閲覧はOK、他人のはADMINのみOK
    @Test
    void viewProfile_selfAllowed() {
        User u = user(1L, User.Role.AUTHOR);
        assertDoesNotThrow(() -> userPolicy.checkViewProfile(u, u));
    }

    // Adminは他人のプロフィール閲覧もOK
    @Test
    void viewProfile_adminAllowedForOthers() {
        User admin = user(1L, User.Role.ADMIN);
        User other = user(2L, User.Role.AUTHOR);
        assertDoesNotThrow(() -> userPolicy.checkViewProfile(admin, other));
    }

    // 非Adminは他人のプロフィール閲覧はNG
    @Test
    void viewProfile_nonAdminDeniedForOthers() {
        User author = user(1L, User.Role.AUTHOR);
        User other = user(2L, User.Role.AUTHOR);
        assertThrows(AccessDeniedException.class, () -> userPolicy.checkViewProfile(author, other));
    }

    // 自分のプロフィール更新はOK、他人のはNG
    @Test
    void updateProfile_onlySelfAllowed() {
        User u = user(3L, User.Role.EDITOR);
        assertDoesNotThrow(() -> userPolicy.checkUpdateProfile(u, u));
        User other = user(4L, User.Role.EDITOR);
        assertThrows(AccessDeniedException.class, () -> userPolicy.checkUpdateProfile(u, other));
    }

    // ユーザー管理はAdminのみOK
    @Test
    void manageUsers_adminOnly() {
        User admin = user(1L, User.Role.ADMIN);
        User non = user(2L, User.Role.AUTHOR);
        assertDoesNotThrow(() -> userPolicy.checkManageUsers(admin));
        assertThrows(AccessDeniedException.class, () -> userPolicy.checkManageUsers(non));
    }

    // ステータス変更はAdminのみOK、自分自身はNG
    @Test
    void changeUserStatus_adminCanChangeOthers_butNotSelf() {
        User admin = user(1L, User.Role.ADMIN);
        User other = user(2L, User.Role.AUTHOR);
        assertDoesNotThrow(() -> userPolicy.checkChangeUserStatus(admin, other));
        // admin cannot change self
        assertThrows(AccessDeniedException.class, () -> userPolicy.checkChangeUserStatus(admin, admin));
        // non-admin cannot change
        User author = user(3L, User.Role.AUTHOR);
        assertThrows(AccessDeniedException.class, () -> userPolicy.checkChangeUserStatus(author, other));
    }

    // ロール変更はAdminのみOK、自分自身はNG
    @Test
    void changeUserRole_adminCanChangeOthers_butNotSelf() {
        User admin = user(10L, User.Role.ADMIN);
        User other = user(11L, User.Role.EDITOR);
        assertDoesNotThrow(() -> userPolicy.checkChangeUserRole(admin, other));
        assertThrows(AccessDeniedException.class, () -> userPolicy.checkChangeUserRole(admin, admin));
        User editor = user(12L, User.Role.EDITOR);
        assertThrows(AccessDeniedException.class, () -> userPolicy.checkChangeUserRole(editor, other));
    }
}
