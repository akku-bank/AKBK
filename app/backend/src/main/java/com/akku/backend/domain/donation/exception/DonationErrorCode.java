package com.akku.backend.domain.donation.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DonationErrorCode implements ErrorCode {
    CHARITY_NOT_FOUND("JEL_001", HttpStatus.NOT_FOUND, "기부처 정보를 찾을 수 없습니다"),
    INSUFFICIENT_JELLING("JEL_002", HttpStatus.BAD_REQUEST, "젤링이 부족합니다"),
    ACTIVE_CHARITY_ALREADY_EXISTS("JEL_003", HttpStatus.BAD_REQUEST, "이미 진행 중인 저금통이 존재합니다");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
