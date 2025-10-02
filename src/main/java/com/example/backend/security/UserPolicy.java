package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class UserPolicy {

    /**
     * プロフィール閲覧権限チェック
     * - 自分のプロフィールは誰でも閲覧可能
     * - 他人のプロフィールは ADMIN のみ閲覧可能
     */
    public void checkViewProfile(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) {
            return; // 自分のプロフィールは閲覧可能
        }
        if (currentUser.getRole() == User.Role.ADMIN) {
            return; // ADMIN は全員のプロフィール閲覧可能
        }
        throw new AccessDeniedException("You can only view your own profile");
    }

    /**
     * プロフィール更新権限チェック
     * - 自分のプロフィールのみ更新可能
     * - ADMIN でも他人のプロフィールは直接更新できない（管理機能で対応）
     */
    public void checkUpdateProfile(User currentUser, User targetUser) {
        if (!currentUser.getId().equals(targetUser.getId())) {
            throw new AccessDeniedException("You can only update your own profile");
        }
    }

    /**
     * ユーザー管理権限チェック
     * - ADMIN のみユーザー管理機能にアクセス可能
     */
    public void checkManageUsers(User currentUser) {
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Only admins can manage users");
        }
    }

    /**
     * ステータス変更権限チェック
     * - ADMIN のみステータス変更可能
     * - 自分自身のステータスは変更できない（アカウントロック防止）
     */
    public void checkChangeUserStatus(User currentUser, User targetUser) {
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Only admins can change user status");
        }
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new AccessDeniedException("You cannot change your own status");
        }
    }

    /**
     * ロール変更権限チェック
     * - ADMIN のみロール変更可能
     * - 自分自身のロールは変更できない（権限剥奪防止）
     */
    public void checkChangeUserRole(User currentUser, User targetUser) {
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Only admins can change user roles");
        }
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new AccessDeniedException("You cannot change your own role");
        }
    }
}
