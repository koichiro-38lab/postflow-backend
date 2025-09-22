
package com.example.backend.service;

import com.example.backend.dto.user.UserMapper;
import com.example.backend.dto.user.UserRequestDto;
import com.example.backend.dto.user.UserResponseDto;
import com.example.backend.entity.User;
import com.example.backend.exception.UserNotFoundException;
import com.example.backend.exception.DuplicateEmailException;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    // Jwtから認証ユーザーを取得（コントローラー共通化用）
    public User getCurrentUser(Jwt jwt) {
        if (jwt == null) {
            throw new com.example.backend.exception.AccessDeniedException("Authentication required");
        }
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.example.backend.exception.AccessDeniedException("Authentication required"));
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto createUser(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new DuplicateEmailException("Email already exists");
        }
        // Roleチェック（サービス層）
        if (dto.role() == null || !isValidRole(dto.role())) {
            throw new com.example.backend.exception.InvalidRoleException("Invalid role");
        }
        User user = new User();
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setRole(User.Role.valueOf(dto.role()));
        User saved = userRepository.save(user);
        return new UserResponseDto(saved.getId(), saved.getEmail(), saved.getRole());
    }

    // 全ユーザー取得
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(UserMapper::toResponseDto)
                .toList();
    }

    // IDでユーザー取得
    public UserResponseDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponseDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 更新
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // メールアドレスが変更されていて、かつ既に存在する場合
        if (!user.getEmail().equals(dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw new DuplicateEmailException("Email already exists");
        }
        // Roleチェック（サービス層）
        if (dto.role() == null || !isValidRole(dto.role())) {
            throw new com.example.backend.exception.InvalidRoleException("Invalid role");
        }
        user.setEmail(dto.email());

        // パスワードが空でなければ更新
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        // ロール更新
        user.setRole(User.Role.valueOf(dto.role()));

        User updated = userRepository.save(user);
        return UserMapper.toResponseDto(updated);
    }

    // 許可されたロールか判定（String型）
    private boolean isValidRole(String role) {
        try {
            User.Role.valueOf(role);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    // 削除
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }
}
