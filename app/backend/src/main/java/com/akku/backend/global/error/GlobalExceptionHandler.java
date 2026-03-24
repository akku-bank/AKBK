package com.akku.backend.global.error;

import com.akku.backend.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
     * 요청 바디 유효성 검사 실패 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String traceId = MDC.get(TRACE_ID_KEY);

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("잘못된 요청입니다.");

        log.warn("[ValidationError] TraceId: {}, Message: {}", traceId, message);

        ApiResponse<Void> response = ApiResponse.fail(message, null, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

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
     * 낙관적 락(Optimistic Lock) 충돌 처리
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException e) {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.warn("[OptimisticLockingFailure] TraceId: {}, Message: {}", traceId, "동시성 충돌로 인한 결제 요청 중단");

        ApiResponse<Void> response = ApiResponse.fail("이미 처리 중이거나 사용된 결제 요청입니다. 다시 시도해 주세요.", null, traceId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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