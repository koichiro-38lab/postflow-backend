package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class PostPolicy {

    public void checkRead(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (resourceOwnerId != null && resourceOwnerId.equals(currentUserId))
                return;
            throw new AccessDeniedException("You can only access your own posts");
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkCreate(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (resourceOwnerId != null && resourceOwnerId.equals(currentUserId))
                return;
            throw new AccessDeniedException("You can only create posts as yourself");
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkUpdate(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        // PUBLISHEDへの変更はService層で事前に判定し、ここではロール・所有者のみ判定
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
                if (actingUserId != null && !actingUserId.equals(currentUserId)) {
                    throw new AccessDeniedException("You don't have permission to change the author");
                }
                return;
            }
            throw new AccessDeniedException("You can only access your own posts");
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkDelete(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (resourceOwnerId != null && resourceOwnerId.equals(currentUserId))
                return;
            throw new AccessDeniedException("You can only access your own posts");
        }
        throw new AccessDeniedException("Access denied");
    }
}
