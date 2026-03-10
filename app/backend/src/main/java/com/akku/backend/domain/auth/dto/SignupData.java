package com.akku.backend.domain.auth.dto;

/**
 * 신규 계정 가입 응답 data
 * signupToken: PIN 설정 단계(/api/auth/signup/pin)에서 사용할 토큰
 */
public record SignupData(String signupToken) {}
