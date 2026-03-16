package com.akku.backend.global.finance.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FinanceErrorCode {
    // 공통 에러
    SUCCESS("H0000", "정상 처리되었습니다."),
    INVALID_HEADER("H1000", "HEADER 정보가 유효하지 않습니다."),
    API_NAME_NOT_FOUND("H1001", "API 이름이 유효하지 않습니다."),
    INVALID_DATE_FORMAT("H1002", "전송일자 형식이 유효하지 않습니다."),
    INVALID_TIME_FORMAT("H1003", "전송시간 형식이 유효하지 않습니다."),
    INVALID_INSTITUTION_CODE("H1004", "기관 코드가 유효하지 않습니다."),
    INVALID_API_KEY("H1008", "API KEY가 유효하지 않습니다."),
    INVALID_USER_KEY("H1009", "USER KEY가 유효하지 않습니다."),

    // 사용자/계좌 관련
    INVALID_DATA_FORMAT("E4001", "입력 데이터가 형식에 맞지 않습니다."),
    USER_ALREADY_EXISTS("E4002", "이미 존재하는 ID입니다."),
    USER_NOT_FOUND("E4003", "존재하지 않는 ID입니다.");

    private final String code;
    private final String description;

    public static FinanceErrorCode fromCode(String code) {
        for (FinanceErrorCode error : values()) {
            if (error.code.equals(code)) {
                return error;
            }
        }
        return null;
    }
}
