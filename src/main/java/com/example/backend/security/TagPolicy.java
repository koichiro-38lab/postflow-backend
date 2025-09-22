package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class TagPolicy {
    public void checkRead(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkCreate(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkUpdate(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkDelete(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }
}
