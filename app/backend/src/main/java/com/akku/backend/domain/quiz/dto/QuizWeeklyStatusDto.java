package com.akku.backend.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 이번 주 퀴즈 챌린지 상태 요약 DTO.
 * QuizWeeklyStatusService → WeeklyChallengesFacade 방향으로 전달된다.
 */
@Getter
@Builder
public class QuizWeeklyStatusDto {

    /**
     * 이번 주(월~일) 동안 solved_date 가 설정된 user_quizzes 레코드 수.
     * 정오답 여부 무관하게 제출 완료(is_submitted=true) 시 카운트된다.
     */
    private int solvedCountThisWeek;

    /**
     * 오늘 퀴즈를 제출 완료했는지 여부 (solved_date = today).
     */
    private boolean isTodaySolved;
}
