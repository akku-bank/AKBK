package com.akku.backend.domain.user.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS("USER_002", HttpStatus.CONFLICT, "이미 가입된 계정입니다"),
    INVALID_PIN_FORMAT("USER_003", HttpStatus.BAD_REQUEST, "간편 비밀번호는 6자리 숫자여야 합니다");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
