package com.akku.backend.domain.user.dto;

/**
 * PATCH /api/users/me 응답 DTO
 */
public record UserUpdateResponse(
        String name
) {}
