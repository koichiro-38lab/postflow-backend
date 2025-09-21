package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class TagPolicy {
    public void checkRead(User.Role role) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR || role == User.Role.AUTHOR) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkCreate(User.Role role) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkUpdate(User.Role role) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkDelete(User.Role role) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }
}
