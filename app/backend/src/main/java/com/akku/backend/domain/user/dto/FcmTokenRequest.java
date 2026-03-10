package com.akku.backend.domain.user.dto;

/**
 * PUT /api/users/me/fcm-token 요청 DTO
 */
public record FcmTokenRequest(
        String fcmToken
) {}
