package com.akku.backend.domain.auth.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    LOGIN_FAILED("AUTH_001", HttpStatus.UNAUTHORIZED, "로그인 실패"),
    UNAUTHORIZED("AUTH_002", HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_TOKEN("AUTH_003", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED("AUTH_004", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    PIN_MISMATCH("AUTH_005", HttpStatus.UNAUTHORIZED, "간편 비밀번호가 일치하지 않습니다"),
    ACCESS_DENIED("AUTH_006", HttpStatus.FORBIDDEN, "접근 권한이 없습니다");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
