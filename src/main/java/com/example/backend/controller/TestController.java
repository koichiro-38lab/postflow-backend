package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    // DTOクラスを定義
    public static class TestResponse {
        public String message;
        public String status;

        public TestResponse(String message, String status) {
            this.message = message;
            this.status = status;
        }
    }

    @GetMapping("/test")
    public TestResponse test() {

        return new TestResponse("Hello World!", "success");
    }
}
