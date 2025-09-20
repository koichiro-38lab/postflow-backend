package com.example.backend.security;

import org.springframework.stereotype.Component;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;

@Component
public class MediaPolicy {

    public void checkRead(User.Role role, Long ownerId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && ownerId != null && ownerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access this media");
    }

    public void checkCreate(User.Role role, Long currentUserId, Long actingUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && currentUserId != null && currentUserId.equals(actingUserId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to upload media");
    }

    public void checkDelete(User.Role role, Long ownerId, Long currentUserId) {
        checkRead(role, ownerId, currentUserId);
    }
}
