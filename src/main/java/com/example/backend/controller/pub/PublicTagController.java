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
 * 公開タグAPIコントローラー。
 * <p>
 * 認証不要で公開投稿に紐づくタグ一覧を取得可能。
 * </p>
 * 
 * @see com.example.backend.service.PublicTagService
 */
@RestController
@RequestMapping("/api/public/tags")
@RequiredArgsConstructor
public class PublicTagController {

    private final PublicTagService publicTagService;

    /**
     * 公開投稿に紐づくタグ一覧を取得。
     * <p>
     * 認証不要。公開状態の投稿に紐づくタグのみ返却。
     * </p>
     * 
     * @return 公開タグのリスト
     */
    @GetMapping
    public ResponseEntity<List<TagPublicResponseDto>> getTags() {
        return ResponseEntity.ok(publicTagService.getPublicTags());
    }
}
