package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.quiz.dto.QuizWeeklyStatusDto;
import com.akku.backend.domain.quiz.repository.UserQuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/**
 * 이번 주 퀴즈 챌린지 상태를 읽기 전용으로 조회하는 서비스.
 *
 * <p>quizzes 테이블은 난이도별 일일 1건 구조(idx_quizzes_created_date_diff).
 * user_quizzes.solved_date 가 NULL 이 아니면 해당 날 퀴즈를 완료한 것으로 간주한다.
 *
 * <p>이 서비스는 quiz 도메인에만 의존하며, challenge 도메인을 참조하지 않는다.
 * WeeklyChallengesFacade(challenge 도메인)가 이 서비스를 주입한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizWeeklyStatusService {

    private final UserQuizRepository userQuizRepository;
    private final Clock clock;

    /**
     * 이번 주 퀴즈 챌린지 상태 조회.
     *
     * <ul>
     *   <li>solvedCountThisWeek: 이번 주 월~일 중 solved_date 가 설정된 레코드 수</li>
     *   <li>isTodaySolved: 오늘 날짜와 일치하는 solved_date 레코드 존재 여부</li>
     * </ul>
     *
     * @param userId 조회 대상 자녀 ID
     */
    public QuizWeeklyStatusDto getThisWeekStatus(UUID userId) {
        LocalDate today      = LocalDate.now(clock);
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisSunday = thisMonday.plusDays(6);

        // GreaterThanEqual(>=) + LessThanEqual(<=) → 월요일·일요일 양쪽 경계값 포함
        int solvedCount = (int) userQuizRepository
                .countByUserIdAndSolvedDateGreaterThanEqualAndSolvedDateLessThanEqual(
                        userId, thisMonday, thisSunday);

        boolean isTodaySolved = userQuizRepository.existsByUserIdAndSolvedDate(userId, today);

        return QuizWeeklyStatusDto.builder()
                .solvedCountThisWeek(solvedCount)
                .isTodaySolved(isTodaySolved)
                .build();
    }
}
