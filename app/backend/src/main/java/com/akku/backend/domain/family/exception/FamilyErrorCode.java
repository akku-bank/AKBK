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
    INVALID_QR_CODE("FAM_002", HttpStatus.BAD_REQUEST, "유효하지 않은 QR 코드입니다."),
    EXPIRED_QR_CODE("FAM_003", HttpStatus.BAD_REQUEST, "만료된 QR 코드입니다. 부모님께 재발급을 요청하세요."),
    QR_ALREADY_EXPIRED("FAM_004", HttpStatus.BAD_REQUEST, "이미 만료되었거나 존재하지 않는 QR 코드입니다."),
    PROFILE_NOT_FOUND("FAM_005", HttpStatus.NOT_FOUND, "해당 가족 구성원 프로필을 찾을 수 없습니다."),
    UNAUTHORIZED_PROFILE_ACCESS("FAM_006", HttpStatus.FORBIDDEN, "해당 가족 그룹에 속한 구성원이 아닙니다."),
    PROFILE_ALREADY_LINKED("FAM_007", HttpStatus.CONFLICT, "일치하는 사전 등록 정보가 없거나 이미 연동된 구성원입니다.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}