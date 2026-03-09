package com.akku.backend.global.dto;

import com.akku.backend.global.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 공통 API 응답 포맷
 * @param <T> 응답 데이터 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        String errorCode,
        String traceId,
        T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, null, null, data);
    }

    public static <T> ApiResponse<T> fail(String message, ErrorCode errorCode, String traceId) {
        return new ApiResponse<>(
                false,
                message,
                errorCode != null ? errorCode.getCode() : "SYS_ERR",
                traceId,
                null
        );
    }
}