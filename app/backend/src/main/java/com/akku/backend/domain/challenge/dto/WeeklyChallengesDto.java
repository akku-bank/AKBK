package com.akku.backend.domain.challenge.dto;

import com.akku.backend.domain.quiz.dto.QuizWeeklyStatusDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/challenges — 이번 주 전체 챌린지 종합 응답 DTO.
 *
 * <pre>
 * {
 *   "esg"     : { challengeId, isCompleted, isRewarded },
 *   "quiz"    : { solvedCountThisWeek, isTodaySolved },
 *   "spending": [ { challengeId, category, targetSpending, ... } ]
 * }
 * </pre>
 *
 * 의존성: challenge → quiz (단방향, QuizWeeklyStatusDto 참조)
 */
public class WeeklyChallengesDto {

    @Getter
    @Builder
    public static class Response {

        /** 이번 주 ESG 챌린지 상태 (Lazy Insert — 없으면 IN_PROGRESS 로 자동 생성) */
        private EsgChallengeDto.StatusResponse esg;

        /** 이번 주 퀴즈 챌린지 상태 요약 */
        private QuizWeeklyStatusDto quiz;

        /** 이번 주 소비 목표 챌린지 목록 (0개 이상) */
        private List<SpendingChallengeDto.ChallengeSummary> spending;
    }
}
