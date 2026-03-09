package com.akku.backend.global.error;

import org.springframework.http.HttpStatus;

/**
 * 전역 예외 처리를 위한 에러 코드 표준 인터페이스
 * 각 도메인별 ErrorCode Enum은 해당 인터페이스를 구현하여 사용
 */
public interface ErrorCode {
    String getCode();
    HttpStatus getStatus();
    String getDefaultMessage();
}