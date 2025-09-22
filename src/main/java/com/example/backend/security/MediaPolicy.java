package com.example.backend.security;

import org.springframework.stereotype.Component;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;

@Component
public class MediaPolicy {

    public void checkRead(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access this media");
    }

    public void checkCreate(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to upload media");
    }

    public void checkDelete(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        checkRead(role, resourceOwnerId, actingUserId, currentUserId);
    }
}
