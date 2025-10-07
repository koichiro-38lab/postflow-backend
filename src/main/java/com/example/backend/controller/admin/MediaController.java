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

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
@Validated
public class MediaController {

    private final MediaService mediaService;
    private final UserRepository userRepository;

    @PostMapping("/presign")
    public ResponseEntity<MediaPresignResponseDto> createPresign(@Valid @RequestBody MediaPresignRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaPresignResponseDto res = mediaService.requestUpload(dto, currentUser);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<MediaResponseDto> register(@Valid @RequestBody MediaCreateRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaResponseDto res = mediaService.register(dto, currentUser);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public Page<MediaResponseDto> list(
            @RequestParam(required = false) String mime,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        return mediaService.list(mime, keyword, pageable, currentUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaResponseDto> getById(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaResponseDto res = mediaService.getById(id, currentUser);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<MediaDownloadResponseDto> download(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        MediaDownloadResponseDto res = mediaService.createDownloadUrl(id, currentUser);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUser(jwt);
        mediaService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private User requireUser(Jwt jwt) {
        if (jwt == null) {
            throw new com.example.backend.exception.AccessDeniedException("Authentication required");
        }
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.example.backend.exception.AccessDeniedException("Authentication required"));
    }
}
