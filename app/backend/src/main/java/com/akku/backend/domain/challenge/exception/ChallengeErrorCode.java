package com.akku.backend.domain.challenge.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 소비 챌린지 도메인 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum ChallengeErrorCode implements ErrorCode {

    CHALLENGE_NOT_FOUND("CHAL_001", HttpStatus.NOT_FOUND, "챌린지 정보를 찾을 수 없습니다"),
    INVALID_STATUS_UPDATE("CHAL_002", HttpStatus.BAD_REQUEST, "상태를 변경할 수 없는 챌린지입니다"),
    ALREADY_REWARDED("CHAL_003", HttpStatus.BAD_REQUEST, "이미 보상을 수령한 챌린지입니다"),
    ALREADY_ATTENDED("CHAL_004", HttpStatus.BAD_REQUEST, "이미 당일 출석이 완료되었습니다"),
    DUPLICATE_CHALLENGE("CHAL_005", HttpStatus.CONFLICT, "해당 주간에 이미 동일한 카테고리의 목표가 등록되어 있습니다."),
    ACCESS_DENIED("CHAL_006", HttpStatus.FORBIDDEN, "해당 챌린지에 대한 접근 권한이 없습니다."),
    REWARD_PERIOD_EXPIRED("CHAL_007", HttpStatus.BAD_REQUEST, "보상 요청 가능 기간이 만료되었습니다. 직전 주 성공 챌린지만 요청 가능합니다."),
    JELLING_NOT_FOUND("CHAL_008", HttpStatus.NOT_FOUND, "젤링 지갑 정보를 찾을 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}