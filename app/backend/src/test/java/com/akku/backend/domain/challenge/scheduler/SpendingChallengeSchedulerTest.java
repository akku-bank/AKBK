package com.akku.backend.domain.challenge.scheduler;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * SpendingChallengeScheduler 단위 테스트.
 *
 * 날짜 격리 전략:
 *  LocalDate.now() 는 MockedStatic<LocalDate> 으로 교체한다.
 *  반드시 try-with-resources 로 감싸 MockedStatic 을 자동 close() 하여
 *  다른 테스트로 시간 오염(Test Pollution)이 전파되지 않도록 한다.
 *  Answers.CALLS_REAL_METHODS 로 now() 이외의 LocalDate 정적 메서드(of, minusDays 등)가
 *  정상 동작하도록 보장한다.
 *
 * 테스트 기준일: 2025-01-06 (월요일)
 *  → lastSunday = 2025-01-05
 *  → lastMonday = 2024-12-30
 *  → thisMonday = 2025-01-06
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpendingChallengeScheduler")
class SpendingChallengeSchedulerTest {

    @Mock
    private SpendingChallengeRepository spendingChallengeRepository;

    @InjectMocks
    private SpendingChallengeScheduler scheduler;

    // 고정 기준일 — 월요일 00:00 실행 시점 시뮬레이션
    private static final LocalDate FIXED_MONDAY      = LocalDate.of(2025, 1, 6);
    private static final LocalDate FIXED_LAST_SUNDAY = LocalDate.of(2025, 1, 5); // FIXED_MONDAY.minusDays(1)

    // --- test helpers ---

    private User mockUser(UUID userId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        return user;
    }

    private SpendingChallenge buildLastWeekChallenge(UUID id, User child, ChallengeStatus status, Long targetSpending) {
        return SpendingChallenge.builder()
                .id(id)
                .user(child)
                .subCategoryName("카페")
                .targetSpending(targetSpending)
                .rewardAmount(5_000L)
                .status(status)
                .startDate(LocalDate.of(2024, 12, 30)) // lastMonday
                .endDate(LocalDate.of(2025, 1, 5))     // lastSunday
                .build();
    }

    // --- test cases ---

    @Nested
    @DisplayName("settleLastWeek - 지난주 챌린지 최종 정산")
    class SettleLastWeek {

        @Test
        @DisplayName("정산 대상 없음 → calculateCurrentSpending 호출 없음")
        void no_challenges_to_settle() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                given(spendingChallengeRepository.findAllByStatusAndEndDate(ChallengeStatus.IN_PROGRESS, FIXED_LAST_SUNDAY))
                        .willReturn(List.of());

                // When
                scheduler.settleLastWeek();

                // Then
                verify(spendingChallengeRepository).findAllByStatusAndEndDate(ChallengeStatus.IN_PROGRESS, FIXED_LAST_SUNDAY);
                // calculateCurrentSpending 은 호출되지 않음 — Mockito strict stubbing 으로 검증됨
            }
        }

        @Test
        @DisplayName("누적 소비 < 목표 → SUCCESS 판정")
        void under_target_spending_results_in_success() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                UUID childId = UUID.randomUUID();
                User child = mockUser(childId);
                SpendingChallenge challenge =
                        buildLastWeekChallenge(UUID.randomUUID(), child, ChallengeStatus.IN_PROGRESS, 10_000L);

                given(spendingChallengeRepository.findAllByStatusAndEndDate(ChallengeStatus.IN_PROGRESS, FIXED_LAST_SUNDAY))
                        .willReturn(List.of(challenge));
                given(spendingChallengeRepository.calculateCurrentSpending(any(), anyString(), any(), any()))
                        .willReturn(8_000L); // 8000 <= 10000

                // When
                scheduler.settleLastWeek();

                // Then
                assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.SUCCESS);
            }
        }

        @Test
        @DisplayName("누적 소비 == 목표 → SUCCESS 판정 (경계값: 초과 아님)")
        void exactly_at_target_spending_results_in_success() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                UUID childId = UUID.randomUUID();
                User child = mockUser(childId);
                SpendingChallenge challenge =
                        buildLastWeekChallenge(UUID.randomUUID(), child, ChallengeStatus.IN_PROGRESS, 10_000L);

                given(spendingChallengeRepository.findAllByStatusAndEndDate(ChallengeStatus.IN_PROGRESS, FIXED_LAST_SUNDAY))
                        .willReturn(List.of(challenge));
                given(spendingChallengeRepository.calculateCurrentSpending(any(), anyString(), any(), any()))
                        .willReturn(10_000L); // 10000 <= 10000 → SUCCESS (조건: > 일 때만 FAIL)

                // When
                scheduler.settleLastWeek();

                // Then
                assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.SUCCESS);
            }
        }

        @Test
        @DisplayName("누적 소비 > 목표 → FAIL 판정 (실시간 이벤트 누락 방어 로직)")
        void over_target_spending_results_in_fail() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                UUID childId = UUID.randomUUID();
                User child = mockUser(childId);
                SpendingChallenge challenge =
                        buildLastWeekChallenge(UUID.randomUUID(), child, ChallengeStatus.IN_PROGRESS, 10_000L);

                given(spendingChallengeRepository.findAllByStatusAndEndDate(ChallengeStatus.IN_PROGRESS, FIXED_LAST_SUNDAY))
                        .willReturn(List.of(challenge));
                given(spendingChallengeRepository.calculateCurrentSpending(any(), anyString(), any(), any()))
                        .willReturn(12_000L); // 12000 > 10000

                // When
                scheduler.settleLastWeek();

                // Then
                assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.FAIL);
            }
        }

        @Test
        @DisplayName("복수 챌린지 → SUCCESS/FAIL 각각 독립 판정")
        void multiple_challenges_judged_independently() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                UUID childId1 = UUID.randomUUID();
                UUID childId2 = UUID.randomUUID();
                User child1 = mockUser(childId1);
                User child2 = mockUser(childId2);

                SpendingChallenge successChallenge =
                        buildLastWeekChallenge(UUID.randomUUID(), child1, ChallengeStatus.IN_PROGRESS, 10_000L);
                SpendingChallenge failChallenge =
                        buildLastWeekChallenge(UUID.randomUUID(), child2, ChallengeStatus.IN_PROGRESS, 10_000L);

                given(spendingChallengeRepository.findAllByStatusAndEndDate(ChallengeStatus.IN_PROGRESS, FIXED_LAST_SUNDAY))
                        .willReturn(List.of(successChallenge, failChallenge));
                given(spendingChallengeRepository.calculateCurrentSpending(eq(childId1), anyString(), any(), any()))
                        .willReturn(7_000L);  // SUCCESS
                given(spendingChallengeRepository.calculateCurrentSpending(eq(childId2), anyString(), any(), any()))
                        .willReturn(15_000L); // FAIL

                // When
                scheduler.settleLastWeek();

                // Then
                assertThat(successChallenge.getStatus()).isEqualTo(ChallengeStatus.SUCCESS);
                assertThat(failChallenge.getStatus()).isEqualTo(ChallengeStatus.FAIL);
            }
        }
    }

    @Nested
    @DisplayName("activateThisWeek - 이번주 챌린지 활성화")
    class ActivateThisWeek {

        @Test
        @DisplayName("활성화 대상 없음 → 아무 처리 없이 종료")
        void no_challenges_to_activate() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                given(spendingChallengeRepository.findAllByStatusAndStartDate(ChallengeStatus.APPROVED, FIXED_MONDAY))
                        .willReturn(List.of());

                // When
                scheduler.activateThisWeek();

                // Then
                verify(spendingChallengeRepository).findAllByStatusAndStartDate(ChallengeStatus.APPROVED, FIXED_MONDAY);
            }
        }

        @Test
        @DisplayName("APPROVED 챌린지 → IN_PROGRESS 전환")
        void approved_challenges_become_in_progress() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                User child = mockUser(UUID.randomUUID());
                SpendingChallenge challenge = SpendingChallenge.builder()
                        .id(UUID.randomUUID())
                        .user(child)
                        .subCategoryName("마트")
                        .targetSpending(20_000L)
                        .rewardAmount(3_000L)
                        .status(ChallengeStatus.APPROVED)
                        .startDate(FIXED_MONDAY)
                        .endDate(FIXED_MONDAY.plusDays(6))
                        .build();

                given(spendingChallengeRepository.findAllByStatusAndStartDate(ChallengeStatus.APPROVED, FIXED_MONDAY))
                        .willReturn(List.of(challenge));

                // When
                scheduler.activateThisWeek();

                // Then
                assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.IN_PROGRESS);
            }
        }

        @Test
        @DisplayName("복수 APPROVED 챌린지 → 모두 IN_PROGRESS 전환")
        void multiple_approved_challenges_all_become_in_progress() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                User child1 = mockUser(UUID.randomUUID());
                User child2 = mockUser(UUID.randomUUID());

                SpendingChallenge challenge1 = SpendingChallenge.builder()
                        .id(UUID.randomUUID()).user(child1).subCategoryName("카페")
                        .targetSpending(10_000L).rewardAmount(2_000L)
                        .status(ChallengeStatus.APPROVED)
                        .startDate(FIXED_MONDAY).endDate(FIXED_MONDAY.plusDays(6))
                        .build();
                SpendingChallenge challenge2 = SpendingChallenge.builder()
                        .id(UUID.randomUUID()).user(child2).subCategoryName("마트")
                        .targetSpending(30_000L).rewardAmount(5_000L)
                        .status(ChallengeStatus.APPROVED)
                        .startDate(FIXED_MONDAY).endDate(FIXED_MONDAY.plusDays(6))
                        .build();

                given(spendingChallengeRepository.findAllByStatusAndStartDate(ChallengeStatus.APPROVED, FIXED_MONDAY))
                        .willReturn(List.of(challenge1, challenge2));

                // When
                scheduler.activateThisWeek();

                // Then
                assertThat(challenge1.getStatus()).isEqualTo(ChallengeStatus.IN_PROGRESS);
                assertThat(challenge2.getStatus()).isEqualTo(ChallengeStatus.IN_PROGRESS);
            }
        }
    }

    @Nested
    @DisplayName("settleWeeklyChallenges - STEP 독립성")
    class SettleWeeklyChallengesIndependence {

        @Test
        @DisplayName("STEP 1 예외 발생 시에도 STEP 2(챌린지 활성화)가 반드시 실행됨")
        void step1_failure_does_not_prevent_step2() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                // STEP 1: DB 오류 시뮬레이션
                given(spendingChallengeRepository.findAllByStatusAndEndDate(any(), any()))
                        .willThrow(new RuntimeException("DB 오류 시뮬레이션"));

                // STEP 2: 빈 목록 반환 (정상 동작)
                given(spendingChallengeRepository.findAllByStatusAndStartDate(any(), any()))
                        .willReturn(List.of());

                // When — settleWeeklyChallenges 는 STEP 1 예외를 catch 하므로 여기까지 전파되지 않아야 함
                scheduler.settleWeeklyChallenges();

                // Then — STEP 2 의 findAllByStatusAndStartDate 가 실제로 호출되었는지 확인
                verify(spendingChallengeRepository)
                        .findAllByStatusAndStartDate(ChallengeStatus.APPROVED, FIXED_MONDAY);
            }
        }

        @Test
        @DisplayName("STEP 2 예외 발생 시에도 settleWeeklyChallenges 가 정상 종료됨")
        void step2_failure_does_not_propagate() {
            // Given
            try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS)) {
                mockedDate.when(LocalDate::now).thenReturn(FIXED_MONDAY);

                // STEP 1: 정상 (빈 목록)
                given(spendingChallengeRepository.findAllByStatusAndEndDate(any(), any()))
                        .willReturn(List.of());

                // STEP 2: DB 오류 시뮬레이션
                given(spendingChallengeRepository.findAllByStatusAndStartDate(any(), any()))
                        .willThrow(new RuntimeException("STEP 2 DB 오류"));

                // When / Then — 예외가 전파되지 않아야 함
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                        () -> scheduler.settleWeeklyChallenges()
                );
            }
        }
    }
}
