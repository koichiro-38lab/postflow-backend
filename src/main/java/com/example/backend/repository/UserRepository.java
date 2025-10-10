package com.example.backend.repository;

import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByRole(User.Role role, Pageable pageable);

    Page<User> findByStatusAndRole(UserStatus status, User.Role role, Pageable pageable);

    boolean existsByAvatarMediaId(Long avatarMediaId);
}
