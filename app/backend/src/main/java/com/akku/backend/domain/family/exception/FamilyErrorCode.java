package com.akku.backend.domain.family.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 가족(Family) 도메인 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum FamilyErrorCode implements ErrorCode {

    FAMILY_NOT_FOUND("FAM_001", HttpStatus.NOT_FOUND, "가족 그룹을 찾을 수 없습니다."),
    INVALID_QR_CODE("FAM_002", HttpStatus.BAD_REQUEST, "유효하지 않은 QR 코드입니다.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}