package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CategoryPolicy {

    public void checkRead(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        // Categoryは全員閲覧可能
    }

    public void checkCreate(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        throw new AccessDeniedException("Only ADMIN or EDITOR can create categories");
    }

    public void checkUpdate(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        throw new AccessDeniedException("Only ADMIN or EDITOR can update categories");
    }

    public void checkDelete(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        throw new AccessDeniedException("Only ADMIN or EDITOR can delete categories");
    }
}