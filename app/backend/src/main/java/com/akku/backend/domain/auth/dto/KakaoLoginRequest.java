package com.akku.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank(message = "소셜 토큰은 필수입니다") String socialToken,
        String fcmToken
) {}
