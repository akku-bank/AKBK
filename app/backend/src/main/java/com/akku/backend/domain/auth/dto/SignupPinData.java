package com.akku.backend.domain.auth.dto;

/**
 * PIN 설정 완료 후 반환되는 데이터 DTO
 */
public record SignupPinData(
        String token,
        String refreshToken
) {}
