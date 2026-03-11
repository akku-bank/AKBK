package com.akku.backend.global.error;

import com.akku.backend.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 비즈니스 예외 처리 (Custom Exception)
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        String traceId = MDC.get(TRACE_ID_KEY);

        log.warn("[ApiException] TraceId: {}, Code: {}, Message: {}", traceId, errorCode.getCode(), e.getMessage());

        ApiResponse<Void> response = ApiResponse.fail(errorCode.getDefaultMessage(), errorCode, traceId);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 서버 내부 오류 처리 (Unhandled Exception)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        String traceId = MDC.get(TRACE_ID_KEY);

        log.error("[Unexpected Error] TraceId: {}", traceId, e);

        ApiResponse<Void> response = ApiResponse.fail("서버 내부 오류가 발생했습니다.", null, traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}