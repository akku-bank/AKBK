package com.akku.backend.domain.challenge.facade;

import com.akku.backend.domain.challenge.dto.EsgChallengeDto;
import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.dto.WeeklyChallengesDto;
import com.akku.backend.domain.challenge.entity.EsgChallengeStatus;
import com.akku.backend.domain.challenge.service.EsgChallengeService;
import com.akku.backend.domain.challenge.service.SpendingChallengeService;
import com.akku.backend.domain.quiz.dto.QuizWeeklyStatusDto;
import com.akku.backend.domain.quiz.service.QuizWeeklyStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * WeeklyChallengesFacade 단위 테스트.
 *
 * 검증 포인트:
 *  - 3개 서비스가 각각 1회씩 호출되는지 (위임 책임)
 *  - 각 서비스의 반환값이 Response 에 그대로 조립되는지 (조립 정확성)
 *  - 소비 챌린지가 0건일 때도 빈 리스트로 정상 조립되는지 (엣지 케이스)
 *
 * 참고: Facade 에 @Transactional 이 없으므로 트랜잭션 경계는 각 서비스 책임.
 *       이 테스트는 조합·위임 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyChallengesFacade")
class WeeklyChallengesFacadeTest {

    @Mock
    private EsgChallengeService esgChallengeService;

    @Mock
    private SpendingChallengeService spendingChallengeService;

    @Mock
    private QuizWeeklyStatusService quizWeeklyStatusService;

    @InjectMocks
    private WeeklyChallengesFacade weeklyChallengesFacade;

    // ─────────────────────────────────────────────────────────────────────────
    // 픽스처 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private EsgChallengeDto.StatusResponse esgResponse(UUID challengeId, EsgChallengeStatus status) {
        return EsgChallengeDto.StatusResponse.builder()
                .challengeId(challengeId)
                .isCompleted(status != EsgChallengeStatus.IN_PROGRESS)
                .isRewarded(status == EsgChallengeStatus.REWARDED)
                .build();
    }

    private QuizWeeklyStatusDto quizResponse(int count, boolean todaySolved) {
        return QuizWeeklyStatusDto.builder()
                .solvedCountThisWeek(count)
                .isTodaySolved(todaySolved)
                .build();
    }

    private SpendingChallengeDto.ChallengeSummary spendingSummary(UUID challengeId) {
        return SpendingChallengeDto.ChallengeSummary.builder()
                .challengeId(challengeId)
                .category("카페")
                .targetSpending(30_000L)
                .rewardAmount(5_000L)
                .status("IN_PROGRESS")
                .startDate(LocalDate.of(2025, 1, 6))
                .endDate(LocalDate.of(2025, 1, 12))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 케이스
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWeeklyChallenges")
    class GetWeeklyChallenges {

        @Test
        @DisplayName("3개 서비스 반환값이 Response 에 정확히 조립된다")
        void assembles_all_three_responses_correctly() {
            // Given
            UUID userId      = UUID.randomUUID();
            UUID esgId       = UUID.randomUUID();
            UUID spendingId  = UUID.randomUUID();

            EsgChallengeDto.StatusResponse esg      = esgResponse(esgId, EsgChallengeStatus.IN_PROGRESS);
            QuizWeeklyStatusDto            quiz     = quizResponse(3, true);
            List<SpendingChallengeDto.ChallengeSummary> spending = List.of(spendingSummary(spendingId));

            given(esgChallengeService.getThisWeekChallenge(userId)).willReturn(esg);
            given(spendingChallengeService.getThisWeekChallenges(userId)).willReturn(spending);
            given(quizWeeklyStatusService.getThisWeekStatus(userId)).willReturn(quiz);

            // When
            WeeklyChallengesDto.Response response = weeklyChallengesFacade.getWeeklyChallenges(userId);

            // Then — 조립 정확성
            assertThat(response.getEsg()).isSameAs(esg);
            assertThat(response.getQuiz()).isSameAs(quiz);
            assertThat(response.getSpending()).isSameAs(spending);
        }

        @Test
        @DisplayName("3개 서비스가 각각 정확히 1회씩 호출된다 (위임 책임 검증)")
        void delegates_to_each_service_exactly_once() {
            // Given
            UUID userId = UUID.randomUUID();

            given(esgChallengeService.getThisWeekChallenge(userId))
                    .willReturn(esgResponse(UUID.randomUUID(), EsgChallengeStatus.SUCCESS));
            given(spendingChallengeService.getThisWeekChallenges(userId))
                    .willReturn(List.of());
            given(quizWeeklyStatusService.getThisWeekStatus(userId))
                    .willReturn(quizResponse(0, false));

            // When
            weeklyChallengesFacade.getWeeklyChallenges(userId);

            // Then
            verify(esgChallengeService).getThisWeekChallenge(userId);
            verify(spendingChallengeService).getThisWeekChallenges(userId);
            verify(quizWeeklyStatusService).getThisWeekStatus(userId);
        }

        @Test
        @DisplayName("이번 주 소비 챌린지 0건 → spending 이 빈 리스트로 조립된다")
        void spending_empty_list_is_assembled_correctly() {
            // Given
            UUID userId = UUID.randomUUID();

            given(esgChallengeService.getThisWeekChallenge(userId))
                    .willReturn(esgResponse(UUID.randomUUID(), EsgChallengeStatus.IN_PROGRESS));
            given(spendingChallengeService.getThisWeekChallenges(userId))
                    .willReturn(List.of()); // 소비 챌린지 없음
            given(quizWeeklyStatusService.getThisWeekStatus(userId))
                    .willReturn(quizResponse(2, false));

            // When
            WeeklyChallengesDto.Response response = weeklyChallengesFacade.getWeeklyChallenges(userId);

            // Then
            assertThat(response.getSpending()).isEmpty();
            assertThat(response.getEsg()).isNotNull();
            assertThat(response.getQuiz()).isNotNull();
        }

        @Test
        @DisplayName("ESG SUCCESS + 퀴즈 오늘 완료 + 소비 2건 → 각 필드 값이 일치한다")
        void full_response_with_all_challenges_active() {
            // Given
            UUID userId     = UUID.randomUUID();
            UUID esgId      = UUID.randomUUID();
            UUID spending1  = UUID.randomUUID();
            UUID spending2  = UUID.randomUUID();

            EsgChallengeDto.StatusResponse esg = esgResponse(esgId, EsgChallengeStatus.SUCCESS);
            QuizWeeklyStatusDto quiz = quizResponse(4, true);
            List<SpendingChallengeDto.ChallengeSummary> spending =
                    List.of(spendingSummary(spending1), spendingSummary(spending2));

            given(esgChallengeService.getThisWeekChallenge(userId)).willReturn(esg);
            given(spendingChallengeService.getThisWeekChallenges(userId)).willReturn(spending);
            given(quizWeeklyStatusService.getThisWeekStatus(userId)).willReturn(quiz);

            // When
            WeeklyChallengesDto.Response response = weeklyChallengesFacade.getWeeklyChallenges(userId);

            // Then
            assertThat(response.getEsg().isCompleted()).isTrue();
            assertThat(response.getEsg().isRewarded()).isFalse();
            assertThat(response.getQuiz().getSolvedCountThisWeek()).isEqualTo(4);
            assertThat(response.getQuiz().isTodaySolved()).isTrue();
            assertThat(response.getSpending()).hasSize(2);
            assertThat(response.getSpending())
                    .extracting(SpendingChallengeDto.ChallengeSummary::getChallengeId)
                    .containsExactlyInAnyOrder(spending1, spending2);
        }
    }
}
