package com.akku.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/users/me 요청 DTO
 * 프로필 수정 (이름 변경)
 */
public record UserUpdateRequest(
        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이내여야 합니다")
        String name
) {}
