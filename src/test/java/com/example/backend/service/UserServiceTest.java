// filepath: backend/src/test/java/com/example/backend/service/UserServiceTest.java
package com.example.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend.dto.user.UserProfileUpdateRequestDto;
import com.example.backend.dto.user.UserRequestDto;
import com.example.backend.dto.user.UserUpdateRequestDto;
import com.example.backend.entity.Media;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.exception.DuplicateEmailException;
import com.example.backend.exception.InvalidRoleException;
import com.example.backend.exception.MediaNotFoundException;
import com.example.backend.exception.UserNotFoundException;
import com.example.backend.repository.MediaRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.UserPolicy;

/**
 * UserService の単体テスト。
 * 境界値テスト、エッジケース、エラーパスを重点的に検証。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserPolicy userPolicy;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User normalUser;
    private Media testMedia;

    @BeforeEach
    void setUp() {
        // 管理者ユーザー
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setDisplayName("Admin User");

        // 通常ユーザー
        normalUser = new User();
        normalUser.setId(2L);
        normalUser.setEmail("user@example.com");
        normalUser.setRole(User.Role.AUTHOR);
        normalUser.setStatus(UserStatus.ACTIVE);
        normalUser.setDisplayName("Normal User");

        // テスト用メディア
        testMedia = new Media();
        testMedia.setId(1L);
        testMedia.setFilename("avatar.jpg");
    }

    // ========== フェーズ2: プロフィール更新の境界値テスト ==========

    /**
     * プロフィール更新: bio が 5000 文字ちょうど (境界値)
     */
    @Test
    void updateMyProfile_bioExactly5000Chars_shouldSucceed() {
        // Arrange
        String bio5000 = "a".repeat(5000);
        UserProfileUpdateRequestDto dto = new UserProfileUpdateRequestDto(
                "Updated Name",
                bio5000,
                null);

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(userPolicy).checkUpdateProfile(any(), any());

        // Act
        userService.updateMyProfile(normalUser, dto);

        // Assert
        verify(userRepository).save(argThat(user -> user.getBio().equals(bio5000) && user.getBio().length() == 5000));
    }

    /**
     * プロフィール更新: displayName が 100 文字ちょうど (境界値)
     */
    @Test
    void updateMyProfile_displayNameExactly100Chars_shouldSucceed() {
        // Arrange
        String displayName100 = "あ".repeat(100); // 日本語で100文字
        UserProfileUpdateRequestDto dto = new UserProfileUpdateRequestDto(
                displayName100,
                "Test bio",
                null);

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(userPolicy).checkUpdateProfile(any(), any());

        // Act
        userService.updateMyProfile(normalUser, dto);

        // Assert
        verify(userRepository).save(
                argThat(user -> user.getDisplayName().equals(displayName100) && user.getDisplayName().length() == 100));
    }

    /**
     * プロフィール更新: displayName が空文字列の場合
     */
    @Test
    void updateMyProfile_displayNameEmpty_shouldSaveAsEmpty() {
        // Arrange
        UserProfileUpdateRequestDto dto = new UserProfileUpdateRequestDto(
                "",
                "Test bio",
                null);

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(userPolicy).checkUpdateProfile(any(), any());

        // Act
        userService.updateMyProfile(normalUser, dto);

        // Assert
        verify(userRepository).save(argThat(user -> user.getDisplayName().isEmpty()));
    }

    // ========== フェーズ2: パスワード変更のエッジケース ==========

    /**
     * パスワード変更: 空白のみのパスワードは変更されない
     */
    @Test
    void updateUserByAdmin_passwordOnlyWhitespace_shouldNotUpdateHash() {
        // Arrange
        String originalHash = "original-hash";
        normalUser.setPasswordHash(originalHash);

        UserUpdateRequestDto dto = new UserUpdateRequestDto(
                null, // email
                "   ", // password (空白のみ)
                null, // displayName
                null, // bio
                null, // avatarMediaId
                null, // role
                null // status
        );

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);

        // Act
        userService.updateUserByAdmin(normalUser.getId(), dto, adminUser);

        // Assert
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(argThat(user -> user.getPasswordHash().equals(originalHash)));
    }

    /**
     * パスワード変更: null パスワードは変更されない
     */
    @Test
    void updateUserByAdmin_passwordNull_shouldNotUpdateHash() {
        // Arrange
        String originalHash = "original-hash";
        normalUser.setPasswordHash(originalHash);

        UserUpdateRequestDto dto = new UserUpdateRequestDto(
                null, // email
                null, // password (null)
                "Updated Name",
                null, // bio
                null, // avatarMediaId
                null, // role
                null // status
        );

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);

        // Act
        userService.updateUserByAdmin(normalUser.getId(), dto, adminUser);

        // Assert
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(argThat(user -> user.getPasswordHash().equals(originalHash)));
    }

    /**
     * パスワード変更: 空文字列パスワードは変更されない
     */
    @Test
    void updateUserByAdmin_passwordEmpty_shouldNotUpdateHash() {
        // Arrange
        String originalHash = "original-hash";
        normalUser.setPasswordHash(originalHash);

        UserUpdateRequestDto dto = new UserUpdateRequestDto(
                null, // email
                "", // password (空文字列)
                null, // displayName
                null, // bio
                null, // avatarMediaId
                null, // role
                null // status
        );

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);

        // Act
        userService.updateUserByAdmin(normalUser.getId(), dto, adminUser);

        // Assert
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(argThat(user -> user.getPasswordHash().equals(originalHash)));
    }

    // ========== フェーズ2: アバターメディア関連のエラーパス ==========

    /**
     * アバターメディア: 存在しないメディアIDを指定した場合、MediaNotFoundExceptionがスローされる
     */
    @Test
    void updateUserByAdmin_nonExistentMediaId_shouldThrowMediaNotFoundException() {
        // Arrange
        Long nonExistentMediaId = 999L;
        UserUpdateRequestDto dto = new UserUpdateRequestDto(
                null, // email
                null, // password
                null, // displayName
                null, // bio
                nonExistentMediaId, // avatarMediaId (存在しない)
                null, // role
                null // status
        );

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(mediaRepository.findById(nonExistentMediaId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserByAdmin(normalUser.getId(), dto, adminUser))
                .isInstanceOf(MediaNotFoundException.class)
                .hasMessageContaining("999");
    }

    /**
     * アバターメディア: 0 を指定した場合、アバターが削除される
     */
    @Test
    void updateUserByAdmin_avatarMediaIdZero_shouldRemoveAvatar() {
        // Arrange
        normalUser.setAvatarMedia(testMedia); // 既存のアバターを設定

        UserUpdateRequestDto dto = new UserUpdateRequestDto(
                null, // email
                null, // password
                null, // displayName
                null, // bio
                0L, // avatarMediaId (0 = 削除)
                null, // role
                null // status
        );

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);

        // Act
        userService.updateUserByAdmin(normalUser.getId(), dto, adminUser);

        // Assert
        verify(mediaRepository, never()).findById(anyLong());
        verify(userRepository).save(argThat(user -> user.getAvatarMedia() == null));
    }

    /**
     * プロフィール更新: avatarMediaId に null を指定した場合、アバターが削除される
     */
    @Test
    void updateMyProfile_avatarMediaIdNull_shouldRemoveAvatar() {
        // Arrange
        normalUser.setAvatarMedia(testMedia); // 既存のアバターを設定

        UserProfileUpdateRequestDto dto = new UserProfileUpdateRequestDto(
                "Updated Name",
                "Updated bio",
                null // avatarMediaId (null = 削除)
        );

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(userPolicy).checkUpdateProfile(any(), any());

        // Act
        userService.updateMyProfile(normalUser, dto);

        // Assert
        verify(mediaRepository, never()).findById(anyLong());
        verify(userRepository).save(argThat(user -> user.getAvatarMedia() == null));
    }

    // ========== フェーズ2: ロールとステータスの組み合わせテスト ==========

    /**
     * ステータス変更: INACTIVE ユーザーのロール変更が可能
     */
    @Test
    void updateUserRole_inactiveUser_shouldSucceed() {
        // Arrange
        normalUser.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(userPolicy).checkChangeUserRole(any(), any());

        // Act
        userService.updateUserRole(normalUser.getId(), User.Role.EDITOR, adminUser);

        // Assert
        verify(userRepository)
                .save(argThat(user -> user.getRole() == User.Role.EDITOR && user.getStatus() == UserStatus.INACTIVE));
    }

    /**
     * ステータス変更: SUSPENDED ユーザーのステータスを ACTIVE に変更可能
     */
    @Test
    void updateUserStatus_suspendedToActive_shouldSucceed() {
        // Arrange
        normalUser.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(userPolicy).checkChangeUserStatus(any(), any());

        // Act
        userService.updateUserStatus(normalUser.getId(), UserStatus.ACTIVE, adminUser);

        // Assert
        verify(userRepository).save(argThat(user -> user.getStatus() == UserStatus.ACTIVE));
    }

    /**
     * 複合テスト: ロールとステータスを同時に変更
     */
    @Test
    void updateUserByAdmin_roleAndStatusSimultaneously_shouldSucceed() {
        // Arrange
        UserUpdateRequestDto dto = new UserUpdateRequestDto(
                null, // email
                null, // password
                null, // displayName
                null, // bio
                null, // avatarMediaId
                "EDITOR", // role
                UserStatus.SUSPENDED // status
        );

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(userPolicy).checkChangeUserRole(any(), any());
        doNothing().when(userPolicy).checkChangeUserStatus(any(), any());

        // Act
        userService.updateUserByAdmin(normalUser.getId(), dto, adminUser);

        // Assert
        verify(userPolicy).checkChangeUserRole(adminUser, normalUser);
        verify(userPolicy).checkChangeUserStatus(adminUser, normalUser);
        verify(userRepository)
                .save(argThat(user -> user.getRole() == User.Role.EDITOR && user.getStatus() == UserStatus.SUSPENDED));
    }

    // ========== 追加のエッジケース ==========

    /**
     * ユーザー作成: 無効なロール文字列でInvalidRoleExceptionがスローされる
     */
    @Test
    void createUser_invalidRole_shouldThrowInvalidRoleException() {
        // Arrange
        UserRequestDto dto = new UserRequestDto(
                "newuser@example.com",
                "password123",
                "INVALID_ROLE", // 無効なロール
                "New User",
                null,
                null);

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        doNothing().when(userPolicy).checkManageUsers(any());

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(dto, adminUser))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("Invalid role");
    }

    /**
     * ユーザー更新: メールアドレス重複で DuplicateEmailException がスローされる
     */
    @Test
    void updateUserByAdmin_duplicateEmail_shouldThrowDuplicateEmailException() {
        // Arrange
        String duplicateEmail = "existing@example.com";
        UserUpdateRequestDto dto = new UserUpdateRequestDto(
                duplicateEmail, // 既に存在するメール
                null,
                null,
                null,
                null,
                null,
                null);

        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.existsByEmail(duplicateEmail)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserByAdmin(normalUser.getId(), dto, adminUser))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email already exists");
    }

    /**
     * ユーザー削除: 存在しないユーザーIDで UserNotFoundException がスローされる
     */
    @Test
    void deleteUser_nonExistentUser_shouldThrowUserNotFoundException() {
        // Arrange
        Long nonExistentUserId = 999L;

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(nonExistentUserId, adminUser))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    /**
     * ユーザー削除: 自分自身の削除は IllegalArgumentException がスローされる
     */
    @Test
    void deleteUser_selfDeletion_shouldThrowIllegalArgumentException() {
        // Arrange
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        doNothing().when(userPolicy).checkManageUsers(any());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(adminUser.getId(), adminUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete your own account");
    }

    /**
     * ユーザー取得: 存在しないユーザーIDで UserNotFoundException がスローされる
     */
    @Test
    void getUserById_nonExistentUser_shouldThrowUserNotFoundException() {
        // Arrange
        Long nonExistentUserId = 999L;

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(nonExistentUserId, adminUser))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
