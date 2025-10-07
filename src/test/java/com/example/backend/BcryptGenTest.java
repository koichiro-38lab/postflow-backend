package com.example.backend;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptGenTest {
    // mainメソッドを実行して、BCryptハッシュが出力されることを確認するテスト
    @Test
    void main_prints_hash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out));
            BcryptGen.main(new String[0]);
            String printed = out.toString();
            assertTrue(printed.contains("BCrypt hash for"), "expected output to contain marker text");
        } finally {
            System.setOut(original);
        }
    }
}
