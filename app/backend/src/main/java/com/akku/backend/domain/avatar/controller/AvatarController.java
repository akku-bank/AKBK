package com.akku.backend.domain.avatar.controller;

import com.akku.backend.domain.avatar.dto.AvatarItemListResponse;
import com.akku.backend.domain.avatar.service.AvatarItemService;
import com.akku.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/avatars")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarItemService avatarItemService;

    /**
     * 아이템 도감 (전체 목록) 조회
     * @param userId 로그인한 유저의 ID (자녀)
     * @param category (선택) 특정 카테고리 필터링
     * @return 도감 아이템 목록 (보유 여부, 레벨 잠금 여부 포함)
     */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<AvatarItemListResponse>> getAvatarItems(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String category
    ) {
        // 1. Service 로직 호출
        AvatarItemListResponse data = avatarItemService.getAvatarItems(userId, category);

        // 2. 공통 응답 포맷(ApiResponse)으로 감싸서 반환
        return ResponseEntity.ok(
                ApiResponse.success("도감 아이템 목록을 불러왔습니다.", data)
        );
    }
}