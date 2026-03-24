package com.akku.backend.domain.social.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SocialErrorCode implements ErrorCode {
    SOC_001("SOC_001", HttpStatus.NOT_FOUND, "친구 정보를 찾을 수 없습니다"),
    SOC_002("SOC_002", HttpStatus.BAD_REQUEST, "이미 등록된 친구입니다"),
    SOC_003("SOC_003", HttpStatus.BAD_REQUEST, "본인에게 친구 요청을 보낼 수 없습니다"),
    SOC_004("SOC_004", HttpStatus.FORBIDDEN, "친구 관계가 아닙니다");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
