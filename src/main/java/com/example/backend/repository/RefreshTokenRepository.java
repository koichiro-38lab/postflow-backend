package com.example.backend.repository;

import com.example.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // user.id 経由での参照
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
