package com.akku.backend.global.error;

import lombok.Getter;

/**
 * 비즈니스 로직 수행 중 발생하는 커스텀 예외
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
}