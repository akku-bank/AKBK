package com.akku.backend.domain.quiz.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 퀴즈(Quiz) 도메인 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum QuizErrorCode implements ErrorCode {

    QUIZ_NOT_FOUND("QZ_001", HttpStatus.NOT_FOUND, "오늘의 퀴즈를 찾을 수 없습니다."),
    DIFFICULTY_CHANGE_NOT_ALLOWED("QZ_002", HttpStatus.CONFLICT, "이미 풀기 시작한 퀴즈의 난이도를 변경할 수 없습니다."),
    QUIZ_ALREADY_SUBMITTED("QZ_003", HttpStatus.CONFLICT, "이미 제출한 퀴즈입니다."),
    AI_SERVER_ERROR("QZ_004", HttpStatus.BAD_GATEWAY, "AI 서버와 통신 중 오류가 발생했습니다."),
    JELLING_NOT_FOUND("QZ_005", HttpStatus.NOT_FOUND, "젤링 지갑 정보를 찾을 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
