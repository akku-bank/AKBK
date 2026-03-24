package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.quiz.dto.QuizWeeklyStatusDto;
import com.akku.backend.domain.quiz.repository.UserQuizRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * QuizWeeklyStatusService 단위 테스트.
 *
 * 검증 포인트:
 *  - 이번 주 퀴즈를 하나도 안 풀었음 → solvedCountThisWeek=0, isTodaySolved=false
 *  - 이번 주 일부 퀴즈 완료, 오늘은 미완료 → count 반영, isTodaySolved=false
 *  - 이번 주 퀴즈 완료, 오늘도 완료 → count 반영, isTodaySolved=true
 *  - 날짜 경계값: thisMonday / thisSunday 양쪽 포함 확인 (GreaterThanEqual + LessThanEqual)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuizWeeklyStatusService")
class QuizWeeklyStatusServiceTest {

    @Mock
    private UserQuizRepository userQuizRepository;

    @InjectMocks
    private QuizWeeklyStatusService quizWeeklyStatusService;

    // ─────────────────────────────────────────────────────────────────────────
    // 공통 날짜 픽스처
    // ─────────────────────────────────────────────────────────────────────────

    private static final LocalDate TODAY       = LocalDate.now();
    private static final LocalDate THIS_MONDAY = TODAY.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private static final LocalDate THIS_SUNDAY = THIS_MONDAY.plusDays(6);

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 케이스
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getThisWeekStatus")
    class GetThisWeekStatus {

        @Test
        @DisplayName("이번 주 퀴즈를 한 번도 풀지 않은 경우 → solvedCountThisWeek=0, isTodaySolved=false")
        void no_quiz_solved_this_week() {
            // Given
            UUID userId = UUID.randomUUID();

            given(userQuizRepository
                    .countByUserIdAndSolvedDateGreaterThanEqualAndSolvedDateLessThanEqual(
                            userId, THIS_MONDAY, THIS_SUNDAY))
                    .willReturn(0L);
            given(userQuizRepository.existsByUserIdAndSolvedDate(userId, TODAY))
                    .willReturn(false);

            // When
            QuizWeeklyStatusDto result = quizWeeklyStatusService.getThisWeekStatus(userId);

            // Then
            assertThat(result.getSolvedCountThisWeek()).isZero();
            assertThat(result.isTodaySolved()).isFalse();
        }

        @Test
        @DisplayName("이번 주 3회 완료, 오늘은 미완료 → solvedCountThisWeek=3, isTodaySolved=false")
        void three_solved_this_week_but_not_today() {
            // Given
            UUID userId = UUID.randomUUID();

            given(userQuizRepository
                    .countByUserIdAndSolvedDateGreaterThanEqualAndSolvedDateLessThanEqual(
                            userId, THIS_MONDAY, THIS_SUNDAY))
                    .willReturn(3L);
            given(userQuizRepository.existsByUserIdAndSolvedDate(userId, TODAY))
                    .willReturn(false);

            // When
            QuizWeeklyStatusDto result = quizWeeklyStatusService.getThisWeekStatus(userId);

            // Then
            assertThat(result.getSolvedCountThisWeek()).isEqualTo(3);
            assertThat(result.isTodaySolved()).isFalse();
        }

        @Test
        @DisplayName("이번 주 퀴즈 완료하고 오늘도 완료 → solvedCountThisWeek 반영, isTodaySolved=true")
        void solved_this_week_and_today() {
            // Given
            UUID userId = UUID.randomUUID();

            given(userQuizRepository
                    .countByUserIdAndSolvedDateGreaterThanEqualAndSolvedDateLessThanEqual(
                            userId, THIS_MONDAY, THIS_SUNDAY))
                    .willReturn(5L);
            given(userQuizRepository.existsByUserIdAndSolvedDate(userId, TODAY))
                    .willReturn(true);

            // When
            QuizWeeklyStatusDto result = quizWeeklyStatusService.getThisWeekStatus(userId);

            // Then
            assertThat(result.getSolvedCountThisWeek()).isEqualTo(5);
            assertThat(result.isTodaySolved()).isTrue();
        }

        @Test
        @DisplayName("날짜 경계값: 조회 시 thisMonday(>=) ~ thisSunday(<=) 를 정확히 전달한다")
        void passes_monday_and_sunday_as_boundary_arguments() {
            // Given
            UUID userId = UUID.randomUUID();

            given(userQuizRepository
                    .countByUserIdAndSolvedDateGreaterThanEqualAndSolvedDateLessThanEqual(
                            userId, THIS_MONDAY, THIS_SUNDAY))
                    .willReturn(0L);
            given(userQuizRepository.existsByUserIdAndSolvedDate(userId, TODAY))
                    .willReturn(false);

            // When
            quizWeeklyStatusService.getThisWeekStatus(userId);

            // Then — 월요일과 일요일이 인자로 정확히 전달됐는지 검증
            verify(userQuizRepository)
                    .countByUserIdAndSolvedDateGreaterThanEqualAndSolvedDateLessThanEqual(
                            userId, THIS_MONDAY, THIS_SUNDAY);
            verify(userQuizRepository).existsByUserIdAndSolvedDate(userId, TODAY);
        }
    }
}
