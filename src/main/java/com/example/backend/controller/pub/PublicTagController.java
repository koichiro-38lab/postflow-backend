package com.example.backend.controller.pub;

import com.example.backend.dto.tag.TagPublicResponseDto;
import com.example.backend.service.PublicTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公開API: タグエンドポイント（認証不要）
 */
@RestController
@RequestMapping("/api/public/tags")
@RequiredArgsConstructor
public class PublicTagController {

    private final PublicTagService publicTagService;

    /**
     * 公開投稿に紐づくタグ一覧を取得
     * GET /api/public/tags
     * 
     * @return 公開タグのリスト
     */
    @GetMapping
    public ResponseEntity<List<TagPublicResponseDto>> getTags() {
        return ResponseEntity.ok(publicTagService.getPublicTags());
    }
}
