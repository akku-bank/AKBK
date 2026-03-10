package com.akku.backend.domain.auth.dto;

public record KakaoLoginRequest(String socialToken, String fcmToken) {}

