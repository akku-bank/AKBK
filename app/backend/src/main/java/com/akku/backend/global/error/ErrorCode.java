package com.akku.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드 정의 (명세 기준)
 */
public enum ErrorCode {

    // ── Auth ─────────────────────────────────────────────────────────────────
    LOGIN_FAILED("AUTH_001", "로그인 실패", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("AUTH_002", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_003", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_004", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED),
    PIN_MISMATCH("AUTH_005", "간편 비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_006", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),

    // ── User ─────────────────────────────────────────────────────────────────
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_002", "이미 가입된 계정입니다", HttpStatus.CONFLICT),
    INVALID_PIN_FORMAT("USER_003", "간편 비밀번호는 6자리 숫자여야 합니다", HttpStatus.BAD_REQUEST),

    // ── Common ───────────────────────────────────────────────────────────────
    BAD_REQUEST("CMM_001", "잘못된 요청 파라미터입니다", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("CMM_999", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
