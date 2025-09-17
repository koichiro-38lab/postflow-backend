package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.example.backend.dto.post.PostRequestDto;

@Component
public class PostPolicy {

    public void checkRead(User.Role role, Long resourceAuthorId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (resourceAuthorId != null && resourceAuthorId.equals(currentUserId))
                return;
            throw new AccessDeniedException("You can only access your own posts");
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkCreate(User.Role role, Long requestedAuthorId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (requestedAuthorId != null && requestedAuthorId.equals(currentUserId))
                return;
            throw new AccessDeniedException("You can only create posts as yourself");
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkUpdate(User.Role role, Long resourceAuthorId, Long requestedAuthorId, Long currentUserId,
            PostRequestDto dto) {
        if (role == User.Role.AUTHOR && "PUBLISHED".equals(dto.getStatus())) {
            throw new AccessDeniedException("Authors cannot publish posts");
        }
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (resourceAuthorId != null && resourceAuthorId.equals(currentUserId)) {
                if (requestedAuthorId != null && !requestedAuthorId.equals(currentUserId)) {
                    throw new AccessDeniedException("You don't have permission to change the author");
                }
                return;
            }
            throw new AccessDeniedException("You can only access your own posts");
        }
        throw new AccessDeniedException("Access denied");
    }

    public void checkDelete(User.Role role, Long resourceAuthorId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR)
            return;
        if (role == User.Role.AUTHOR) {
            if (resourceAuthorId != null && resourceAuthorId.equals(currentUserId))
                return;
            throw new AccessDeniedException("You can only access your own posts");
        }
        throw new AccessDeniedException("Access denied");
    }
}
