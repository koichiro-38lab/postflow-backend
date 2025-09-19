
package com.example.backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGen {
    public static void main(String[] args) {
        String password = "password123";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        System.out.println("BCrypt hash for '" + password + "':");
        System.out.println(hash);
    }
}
