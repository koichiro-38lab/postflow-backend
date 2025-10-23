// filepath: backend/src/main/java/com/example/backend/controller/admin/AdminDemoController.java
package com.example.backend.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.batch.DemoContentResetScheduler;

/**
 * デモ環境管理用のコントローラ。
 * フルシードの手動実行エンドポイントを提供。
 */
@RestController
@RequestMapping("/api/admin/demo")
public class AdminDemoController {

    private final DemoContentResetScheduler demoContentResetScheduler;

    public AdminDemoController(DemoContentResetScheduler demoContentResetScheduler) {
        this.demoContentResetScheduler = demoContentResetScheduler;
    }

    /**
     * フルシードを手動実行（ADMIN権限必須）。
     * 投稿100件・メディア多数を投入し、定期実行と同じ処理を実行。
     * 
     * @return 204 No Content
     */
    @PostMapping("/reset-full")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetFullDemoContent() {
        demoContentResetScheduler.executeFullSeed();
        return ResponseEntity.noContent().build();
    }
}
