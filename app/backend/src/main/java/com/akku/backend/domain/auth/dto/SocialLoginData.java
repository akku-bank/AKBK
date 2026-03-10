package com.akku.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 소셜 로그인 응답 data 필드
 * - 신규 유저: isRegistered=false, tempToken 포함
 * - 기존 유저: isRegistered=true, token + refreshToken 포함
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SocialLoginData(
        boolean isRegistered,
        String tempToken,
        String token,
        String refreshToken
) {
    // 신규 유저 팩토리
    public static SocialLoginData newUser(String tempToken) {
        return new SocialLoginData(false, tempToken, null, null);
    }

    // 기존 유저 팩토리
    public static SocialLoginData existingUser(String token, String refreshToken) {
        return new SocialLoginData(true, null, token, refreshToken);
    }
}
