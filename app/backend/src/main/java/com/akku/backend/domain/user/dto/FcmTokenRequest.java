package com.akku.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * PUT /api/users/me/fcm-token 요청 DTO
 */
public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다") String fcmToken
) {}
