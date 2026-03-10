package com.akku.backend.domain.user.dto;

/**
 * PATCH /api/users/me 요청 DTO
 * 모든 필드 선택값 (부분 수정 가능)
 */
public record UserUpdateRequest(
        String name,
        String fcmToken
) {}
