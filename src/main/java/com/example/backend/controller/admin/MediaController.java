package com.example.backend.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.media.MediaCreateRequestDto;
import com.example.backend.dto.media.MediaDownloadResponseDto;
import com.example.backend.dto.media.MediaPresignRequestDto;
import com.example.backend.dto.media.MediaPresignResponseDto;
import com.example.backend.dto.media.MediaResponseDto;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.MediaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 管理画面用メディアAPIコントローラー。
 * <p>
 * メディアのアップロード事前署名、登録、一覧取得、詳細取得、ダウンロードURL発行、削除を提供。
 * 全エンドポイントで認証・RBAC制御を行う。
 * </p>
 * <ul>
 * <li>アップロード事前署名: S3/MinIO用presign URL発行</li>
 * <li>登録: presign後のメタ情報登録</li>
 * <li>一覧: MIME/キーワード/ページング対応</li>
 * <li>詳細/ダウンロード: アクセス権チェック</li>
 * <li>削除: RBAC・参照整合性</li>
 * </ul>
 * 
 * @see com.example.backend.service.MediaService
 * @see com.example.backend.security.MediaPolicy
 */
@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
@Validated
public class MediaController {

    private final MediaService mediaService;
    private final UserRepository userRepository;

    /**
     * アップロード用の事前署名URLを発行。
     * <p>
     * S3/MinIO等のストレージに直接アップロードするためのpresign URLを返す。
     * </p>
     * 
     * @param dto アップロードリクエスト情報
     * @param jwt JWT認証情報
     * @return 事前署名URLレスポンス
     * @throws com.example.backend.exception.AccessDeniedException          認証・権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping("/presign")
    public ResponseEntity<MediaPresignResponseDto> createPresign(@Valid @RequestBody MediaPresignRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaPresignResponseDto res = mediaService.requestUpload(dto, currentUser);
        return ResponseEntity.ok(res);
    }

    /**
     * presign後のメディア情報を登録。
     * <p>
     * presignでアップロードしたファイルのメタ情報をDBに登録。
     * </p>
     * 
     * @param dto メディア登録リクエスト情報
     * @param jwt JWT認証情報
     * @return 登録されたメディア情報
     * @throws com.example.backend.exception.AccessDeniedException          認証・権限不足
     * @throws org.springframework.web.bind.MethodArgumentNotValidException バリデーションエラー
     */
    @PostMapping
    public ResponseEntity<MediaResponseDto> register(@Valid @RequestBody MediaCreateRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaResponseDto res = mediaService.register(dto, currentUser);
        return ResponseEntity.ok(res);
    }

    /**
     * メディア一覧を取得。
     * <p>
     * MIMEタイプ・キーワードでフィルタ可能。ページング対応。
     * </p>
     * 
     * @param mime     MIMEタイプフィルター（任意）
     * @param keyword  キーワードフィルター（任意）
     * @param pageable ページング情報
     * @param jwt      JWT認証情報
     * @return メディアのページ
     * @throws com.example.backend.exception.AccessDeniedException 認証・権限不足
     */
    @GetMapping
    public Page<MediaResponseDto> list(
            @RequestParam(required = false) String mime,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        return mediaService.list(mime, keyword, pageable, currentUser);
    }

    /**
     * ID指定でメディア詳細を取得。
     * <p>
     * 存在しない場合は404、権限不足は403。
     * </p>
     * 
     * @param id  メディアID
     * @param jwt JWT認証情報
     * @return メディア情報
     * @throws com.example.backend.exception.AccessDeniedException  権限不足
     * @throws com.example.backend.exception.MediaNotFoundException 存在しない場合
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaResponseDto> getById(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaResponseDto res = mediaService.getById(id, currentUser);
        return ResponseEntity.ok(res);
    }

    /**
     * メディアダウンロード用の一時URLを発行。
     * <p>
     * S3/MinIO等のpresignダウンロードURLを返す。
     * </p>
     * 
     * @param id  メディアID
     * @param jwt JWT認証情報
     * @return ダウンロードURLレスポンス
     * @throws com.example.backend.exception.AccessDeniedException  権限不足
     * @throws com.example.backend.exception.MediaNotFoundException 存在しない場合
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<MediaDownloadResponseDto> download(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaDownloadResponseDto res = mediaService.createDownloadUrl(id, currentUser);
        return ResponseEntity.ok(res);
    }

    /**
     * ID指定でメディアを削除。
     * <p>
     * 投稿等で参照されている場合は409（MediaInUseException）、存在しない場合は404。
     * </p>
     * 
     * @param id  メディアID
     * @param jwt JWT認証情報
     * @return 204 No Content
     * @throws com.example.backend.exception.MediaInUseException    参照整合性違反時
     * @throws com.example.backend.exception.MediaNotFoundException 存在しない場合
     * @throws com.example.backend.exception.AccessDeniedException  権限不足
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        mediaService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * JWTから認証ユーザーを取得。
     * <p>
     * 認証情報が無い場合やユーザーが見つからない場合はAccessDeniedException。
     * </p>
     * 
     * @param jwt JWT認証情報
     * @return 認証済みユーザー
     * @throws com.example.backend.exception.AccessDeniedException 認証失敗時
     */
    private User requireUser(Jwt jwt) {
        if (jwt == null) {
            throw new com.example.backend.exception.AccessDeniedException("Authentication required");
        }
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.example.backend.exception.AccessDeniedException("Authentication required"));
    }
}
