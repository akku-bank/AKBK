package com.akku.backend.domain.challenge.event;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CardPaymentEventListener 단위 테스트.
 *
 * 검증 포인트:
 *  - 매칭 챌린지 없음 → calculateCurrentSpending 호출 없음
 *  - 누적 소비 <= 목표 → 상태 변경 없음 (IN_PROGRESS 유지)
 *  - 누적 소비 == 목표 경계값 → 초과 아님, 상태 유지
 *  - 누적 소비 > 목표 → FAIL 판정 (updateStatus 호출)
 *
 * 참고: @Async, @TransactionalEventListener 는 Spring 컨텍스트 없이 직접 메서드를 호출하므로
 *       어노테이션 동작(비동기, 트랜잭션 타이밍)은 이 단위 테스트 범위 밖이다.
 *       해당 동작은 통합 테스트에서 검증해야 한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CardPaymentEventListener")
class CardPaymentEventListenerTest {

    @Mock
    private SpendingChallengeRepository spendingChallengeRepository;

    @InjectMocks
    private CardPaymentEventListener cardPaymentEventListener;

    // --- test helpers ---

    private User mockUser(UUID userId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        return user;
    }

    /**
     * startDate=2025-01-06(월), endDate=2025-01-12(일) 고정 — 날짜 파생값 예측 가능
     */
    private SpendingChallenge buildChallenge(UUID id, User child, Long targetSpending) {
        return SpendingChallenge.builder()
                .id(id)
                .user(child)
                .subCategoryName("카페")
                .targetSpending(targetSpending)
                .rewardAmount(5_000L)
                .status(ChallengeStatus.IN_PROGRESS)
                .startDate(LocalDate.of(2025, 1, 6))
                .endDate(LocalDate.of(2025, 1, 12))
                .build();
    }

    private CardPaymentEvent buildEvent(UUID userId, String category) {
        return new CardPaymentEvent(userId, "123-456", "35796", "withdraw-123", category, 3_000L, 97_000L, "1", "CU", LocalDate.of(2025, 1, 8), "123456", false);
    }

    // --- test cases ---

    @Nested
    @DisplayName("onCardPayment")
    class OnCardPayment {

        @Test
        @DisplayName("매칭 챌린지 없음 → calculateCurrentSpending 호출 없음")
        void no_matching_challenge_skips_spending_calculation() {
            // Given
            UUID userId = UUID.randomUUID();
            CardPaymentEvent event = buildEvent(userId, "카페");

            given(spendingChallengeRepository.findActiveChallenge(any(), anyString(), any(), any()))
                    .willReturn(Optional.empty());

            // When
            cardPaymentEventListener.onCardPayment(event);

            // Then
            verify(spendingChallengeRepository, never())
                    .calculateCurrentSpending(any(), anyString(), any(), any());
        }

        @Test
        @DisplayName("누적 소비 < 목표 → 챌린지 상태 IN_PROGRESS 유지")
        void spending_under_target_keeps_challenge_in_progress() {
            // Given
            UUID childId = UUID.randomUUID();
            User child = mockUser(childId);
            SpendingChallenge challenge = buildChallenge(UUID.randomUUID(), child, 10_000L);
            CardPaymentEvent event = buildEvent(childId, "카페");

            given(spendingChallengeRepository.findActiveChallenge(any(), anyString(), any(), any()))
                    .willReturn(Optional.of(challenge));
            given(spendingChallengeRepository.calculateCurrentSpending(any(), anyString(), any(), any()))
                    .willReturn(9_000L); // 9000 < 10000 → 통과

            // When
            cardPaymentEventListener.onCardPayment(event);

            // Then
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("누적 소비 == 목표 → 초과 아님, 챌린지 상태 IN_PROGRESS 유지 (경계값)")
        void spending_exactly_at_target_keeps_challenge_in_progress() {
            // Given
            UUID childId = UUID.randomUUID();
            User child = mockUser(childId);
            SpendingChallenge challenge = buildChallenge(UUID.randomUUID(), child, 10_000L);
            CardPaymentEvent event = buildEvent(childId, "카페");

            given(spendingChallengeRepository.findActiveChallenge(any(), anyString(), any(), any()))
                    .willReturn(Optional.of(challenge));
            given(spendingChallengeRepository.calculateCurrentSpending(any(), anyString(), any(), any()))
                    .willReturn(10_000L); // 10000 == 10000 → NOT > → FAIL 아님

            // When
            cardPaymentEventListener.onCardPayment(event);

            // Then
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("누적 소비 > 목표 → 챌린지 FAIL 판정")
        void spending_over_target_fails_challenge() {
            // Given
            UUID childId = UUID.randomUUID();
            User child = mockUser(childId);
            SpendingChallenge challenge = buildChallenge(UUID.randomUUID(), child, 10_000L);
            CardPaymentEvent event = buildEvent(childId, "카페");

            given(spendingChallengeRepository.findActiveChallenge(any(), anyString(), any(), any()))
                    .willReturn(Optional.of(challenge));
            given(spendingChallengeRepository.calculateCurrentSpending(any(), anyString(), any(), any()))
                    .willReturn(10_001L); // 10001 > 10000 → FAIL

            // When
            cardPaymentEventListener.onCardPayment(event);

            // Then
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.FAIL);
        }
    }
}
