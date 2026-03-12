package com.akku.backend.domain.user.dto;

import java.util.UUID;

/**
 * GET /api/users/me 응답 DTO
 */
public record UserProfileResponse(
        UUID userId,
        String role,
        String name,
        UUID familyId,
        Integer level
) {}
