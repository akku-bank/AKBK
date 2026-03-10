package com.akku.backend.domain.auth.dto;

/**
 * 토큰 재발급 응답 data
 * token: 새로 발급된 JWT Access Token
 */
public record RefreshData(String token) {}
