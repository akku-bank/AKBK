package com.akku.backend.domain.user.dto;

/**
 * PATCH /api/users/me 요청 DTO
 * 프로필 수정 (이름 변경)
 */
public record UserUpdateRequest(
        String name
) {}
