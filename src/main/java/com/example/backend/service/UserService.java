package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ユーザー作成
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // 全件取得
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // IDで検索
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Emailで検索
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // 更新
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // 削除
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
