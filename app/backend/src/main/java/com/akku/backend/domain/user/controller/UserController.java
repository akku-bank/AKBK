package com.akku.backend.domain.user.controller;

import com.akku.backend.domain.user.dto.FcmTokenRequest;
import com.akku.backend.domain.user.dto.UserProfileResponse;
import com.akku.backend.domain.user.dto.UserUpdateRequest;
import com.akku.backend.domain.user.dto.UserUpdateResponse;
import com.akku.backend.global.dto.ApiResponse;
import com.akku.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/users/me
     * Header: Authorization: Bearer {token}
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UUID userId
    ) {
        UserProfileResponse data = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 성공", data));
    }

    /**
     * 내 정보 수정
     * PATCH /api/users/me
     * Header: Authorization: Bearer {token}
     * Body: { "name": "변경할 이름" }
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserUpdateResponse>> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @RequestBody UserUpdateRequest request
    ) {
        UserUpdateResponse data = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("프로필 수정이 완료되었습니다.", data));
    }

    /**
     * FCM 토큰 갱신
     * PUT /api/users/me/fcm-token
     * Header: Authorization: Bearer {token}
     * Body: { "fcmToken": "new_device_token" }
     */
    @PutMapping("/me/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal UUID userId,
            @RequestBody FcmTokenRequest request
    ) {
        userService.updateFcmToken(userId, request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 갱신되었습니다.", null));
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     * DELETE /api/users/me
     * Header: Authorization: Bearer {token}
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UUID userId
    ) {
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success(
                "회원 탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.",
                null
        ));
    }
}
