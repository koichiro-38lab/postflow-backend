package com.example.backend.service;

import com.example.backend.dto.user.UserMapper;
import com.example.backend.dto.user.UserRequestDto;
import com.example.backend.dto.user.UserResponseDto;
import com.example.backend.dto.user.UserProfileResponseDto;
import com.example.backend.dto.user.UserProfileUpdateRequestDto;
import com.example.backend.dto.user.UserUpdateRequestDto;
import com.example.backend.entity.Media;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.exception.UserNotFoundException;
import com.example.backend.exception.DuplicateEmailException;
import com.example.backend.exception.MediaNotFoundException;
import com.example.backend.repository.MediaRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.UserPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.LocalDateTime;
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
    private final MediaRepository mediaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPolicy userPolicy;

    /**
     * ユーザー作成
     * 
     * @param dto
     * @param currentUser
     * @return
     */
    @Transactional
    public UserResponseDto createUser(UserRequestDto dto, User currentUser) {
        // 権限チェック（管理者のみユーザー作成可能）
        userPolicy.checkManageUsers(currentUser);

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
        user.setStatus(UserStatus.ACTIVE);
        user.setDisplayName(dto.displayName());
        user.setBio(dto.bio());
        user.setAvatarMedia(dto.avatarMediaId() != null ? mediaRepository.findById(dto.avatarMediaId())
                .orElseThrow(() -> new MediaNotFoundException(dto.avatarMediaId())) : null);
        User saved = userRepository.save(user);
        return UserMapper.toResponseDto(saved);
    }

    /**
     * 全ユーザー取得（管理者用）
     * 
     * @return
     */
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(UserMapper::toResponseDto)
                .toList();
    }

    // IDでユーザー取得
    public UserResponseDto getUserById(Long id, User currentUser) {
        // 権限チェック（管理者のみ他人のユーザー情報取得可能）
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        userPolicy.checkViewProfile(currentUser, targetUser);

        return UserMapper.toResponseDto(targetUser);
    }

    /**
     * ユーザー更新
     * 
     * @param id
     * @param dto
     * @return
     */
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

    /**
     * 管理者によるユーザー更新
     * 
     * @param userId
     * @param dto
     * @param currentUser
     * @return
     */
    @Transactional
    public UserResponseDto updateUserByAdmin(Long userId, UserUpdateRequestDto dto, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // ロール変更の権限チェック
        if (dto.role() != null) {
            userPolicy.checkChangeUserRole(currentUser, user);
        }

        // ステータス変更の権限チェック
        if (dto.status() != null) {
            userPolicy.checkChangeUserStatus(currentUser, user);
        }

        // メールアドレスが変更されていて、かつ既に存在する場合
        if (dto.email() != null && !user.getEmail().equals(dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw new DuplicateEmailException("Email already exists");
        } // メールアドレス更新
        if (dto.email() != null) {
            user.setEmail(dto.email());
        }

        // パスワードが空でなければ更新
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        // 表示名更新
        if (dto.displayName() != null) {
            user.setDisplayName(dto.displayName());
        }

        // 自己紹介更新
        if (dto.bio() != null) {
            user.setBio(dto.bio());
        }

        // アバター更新
        if (dto.avatarMediaId() != null) {
            if (dto.avatarMediaId() == 0) {
                // 0 の場合はアバターを削除
                user.setAvatarMedia(null);
            } else {
                // IDからMediaを取得して設定
                Media media = mediaRepository.findById(dto.avatarMediaId())
                        .orElseThrow(() -> new MediaNotFoundException(dto.avatarMediaId()));
                user.setAvatarMedia(media);
            }
        }

        // ロール更新
        if (dto.role() != null) {
            if (!isValidRole(dto.role())) {
                throw new IllegalArgumentException("Invalid role: " + dto.role());
            }
            user.setRole(User.Role.valueOf(dto.role()));
        }

        // ステータス更新
        if (dto.status() != null) {
            user.setStatus(dto.status());
        }

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

    /**
     * 自分のプロフィール取得
     * 
     * @param currentUser
     * @return
     */
    public UserProfileResponseDto getMyProfile(User currentUser) {
        // 最新の情報を取得（リレーションも含む）
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserMapper.toProfileResponseDto(user);
    }

    /**
     * 自分のプロフィール更新
     * 
     * @param currentUser
     * @param dto
     * @return
     */
    @Transactional
    public UserProfileResponseDto updateMyProfile(User currentUser, UserProfileUpdateRequestDto dto) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 権限チェック（自分自身のプロフィールのみ更新可能）
        userPolicy.checkUpdateProfile(currentUser, user);

        // プロフィール情報の更新
        UserMapper.applyProfileUpdate(user, dto);

        // アバター画像の設定
        if (dto.avatarMediaId() != null) {
            Media media = mediaRepository.findById(dto.avatarMediaId())
                    .orElseThrow(() -> new MediaNotFoundException(dto.avatarMediaId()));
            user.setAvatarMedia(media);
        } else {
            user.setAvatarMedia(null);
        }

        User updated = userRepository.save(user);
        return UserMapper.toProfileResponseDto(updated);
    }

    /**
     * 最終ログイン日時の更新
     * 
     * @param user
     */
    @Transactional
    public void updateLastLoginAt(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * ページネーション付きで全ユーザー取得（管理者用）
     * 
     * @param pageable
     * @param status
     * @param role
     * @param currentUser
     * @return
     */
    public Page<UserResponseDto> findAllWithPagination(Pageable pageable, UserStatus status, User.Role role,
            User currentUser) {
        // 権限チェック（管理者のみユーザー一覧を取得可能）
        userPolicy.checkManageUsers(currentUser);

        Page<User> users;
        if (status != null && role != null) {
            users = userRepository.findByStatusAndRole(status, role, pageable);
        } else if (status != null) {
            users = userRepository.findByStatus(status, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(UserMapper::toResponseDto);
    }

    /**
     * ユーザーステータス変更
     * 
     * @param userId
     * @param status
     * @param currentUser
     * @return
     */
    @Transactional
    public UserResponseDto updateUserStatus(Long userId, UserStatus status, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 権限チェック
        userPolicy.checkChangeUserStatus(currentUser, user);

        user.setStatus(status);
        User updated = userRepository.save(user);
        return UserMapper.toResponseDto(updated);
    }

    /**
     * ユーザーロール変更
     * 
     * @param userId
     * @param role
     * @param currentUser
     * @return
     */
    @Transactional
    public UserResponseDto updateUserRole(Long userId, User.Role role, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 権限チェック
        userPolicy.checkChangeUserRole(currentUser, user);

        user.setRole(role);
        User updated = userRepository.save(user);
        return UserMapper.toResponseDto(updated);
    }

    /**
     * ユーザー削除
     * 
     * @param userId
     * @param currentUser
     */
    @Transactional
    public void deleteUser(Long userId, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 権限チェック（管理者のみ削除可能）
        userPolicy.checkManageUsers(currentUser);

        // 自分自身の削除を防止
        if (userId.equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }

        userRepository.delete(user);
    }
}
