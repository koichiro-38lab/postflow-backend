package com.example.backend.security;

import org.springframework.stereotype.Component;

import com.example.backend.entity.User;
import com.example.backend.exception.AccessDeniedException;
import com.example.backend.repository.UserRepository;

@Component
public class MediaPolicy {

    private final UserRepository userRepository;

    public MediaPolicy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void checkRead(User.Role role, Long resourceOwnerId, Long actingUserId, Long currentUserId) {
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access this media");
    }

    public void checkReadForMedia(User.Role role, Long mediaId, Long resourceOwnerId, Long currentUserId) {
        // ADMIN、EDITORは全てのメディアを閲覧可能
        if (role == User.Role.ADMIN || role == User.Role.EDITOR) {
            return;
        }

        // 自分がアップロードしたメディアは閲覧可能
        if (role == User.Role.AUTHOR && resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }

        // いずれかのユーザーのアバターとして使用されているメディアは誰でも閲覧可能
        if (mediaId != null && userRepository.existsByAvatarMediaId(mediaId)) {
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
