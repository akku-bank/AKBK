package com.akku.backend.domain.challenge.event;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.challenge.entity.EsgChallenge;
import com.akku.backend.domain.challenge.entity.EsgChallengeStatus;
import com.akku.backend.domain.challenge.repository.EsgChallengeRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * EsgCardPaymentEventListener 단위 테스트.
 *
 * 검증 포인트:
 *  - isGreen=false → 챌린지 조회 자체를 수행하지 않음 (early exit)
 *  - isGreen=true, IN_PROGRESS 챌린지 존재 → markSuccess() 호출 → SUCCESS 상태
 *  - isGreen=true, 챌린지 없음 → 무시 (예외 없음)
 *  - isGreen=true, 이미 SUCCESS 상태 → 조회 결과 없으므로 무시
 *
 * 참고: @Async, @TransactionalEventListener 는 Spring 컨텍스트 없이 직접 메서드를 호출하므로
 *       어노테이션 동작(비동기, 트랜잭션 타이밍)은 이 단위 테스트 범위 밖이다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EsgCardPaymentEventListener")
class EsgCardPaymentEventListenerTest {

    @Mock
    private EsgChallengeRepository esgChallengeRepository;

    @InjectMocks
    private EsgCardPaymentEventListener listener;

    // ─────────────────────────────────────────────────────────────────────────
    // 공통 픽스처 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 결제일이 속한 주의 월요일을 계산 — 서비스 내부 로직과 동일하게 유지.
     */
    private static LocalDate toMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private CardPaymentEvent buildEvent(UUID userId, boolean isGreen) {
        return new CardPaymentEvent(userId, "123-456", "35796", "withdraw-123", "편의점", 5_000L, 95_000L, "1", "편의점", LocalDate.of(2025, 1, 8), "123456", isGreen);
    }

    private EsgChallenge buildChallenge(UUID challengeId, User user, EsgChallengeStatus status) {
        return EsgChallenge.builder()
                .id(challengeId)
                .user(user)
                .rewardAmount(50L)
                .status(status)
                .startDate(LocalDate.of(2025, 1, 6))  // 2025-01-08의 해당 주 월요일
                .endDate(LocalDate.of(2025, 1, 12))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 케이스
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onCardPayment")
    class OnCardPayment {

        @Test
        @DisplayName("isGreen=false → 챌린지 조회 없이 즉시 종료 (early exit)")
        void non_green_merchant_skips_all_processing() {
            // Given
            UUID userId = UUID.randomUUID();
            CardPaymentEvent event = buildEvent(userId, false);

            // When
            listener.onCardPayment(event);

            // Then — DB 조회 전혀 없어야 함
            verify(esgChallengeRepository, never())
                    .findByUserIdAndStartDateAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("isGreen=true, IN_PROGRESS 챌린지 존재 → markSuccess() 호출 → SUCCESS 상태로 변경")
        void green_merchant_marks_in_progress_challenge_as_success() {
            // Given
            UUID userId = UUID.randomUUID();
            User child = User.builder().id(userId).build();

            CardPaymentEvent event = buildEvent(userId, true);
            LocalDate expectedMonday = toMonday(event.transactionDate()); // 2025-01-06

            EsgChallenge challenge = buildChallenge(UUID.randomUUID(), child, EsgChallengeStatus.IN_PROGRESS);

            given(esgChallengeRepository.findByUserIdAndStartDateAndStatus(
                    userId, expectedMonday, EsgChallengeStatus.IN_PROGRESS))
                    .willReturn(Optional.of(challenge));

            // When
            listener.onCardPayment(event);

            // Then
            assertThat(challenge.getStatus()).isEqualTo(EsgChallengeStatus.SUCCESS);
        }

        @Test
        @DisplayName("isGreen=true, 이번 주 IN_PROGRESS 챌린지 없음 → 무시 (예외 없음)")
        void green_merchant_with_no_active_challenge_does_nothing() {
            // Given
            UUID userId = UUID.randomUUID();
            CardPaymentEvent event = buildEvent(userId, true);
            LocalDate expectedMonday = toMonday(event.transactionDate());

            given(esgChallengeRepository.findByUserIdAndStartDateAndStatus(
                    userId, expectedMonday, EsgChallengeStatus.IN_PROGRESS))
                    .willReturn(Optional.empty());

            // When & Then — 예외 없이 정상 종료
            listener.onCardPayment(event);
        }

        @Test
        @DisplayName("isGreen=true, 이미 SUCCESS 상태 → 리포지토리는 IN_PROGRESS만 조회하므로 무시")
        void green_merchant_with_already_succeeded_challenge_does_nothing() {
            // Given — 이미 SUCCESS인 챌린지는 findByUserIdAndStartDateAndStatus에서 반환되지 않음
            UUID userId = UUID.randomUUID();
            CardPaymentEvent event = buildEvent(userId, true);
            LocalDate expectedMonday = toMonday(event.transactionDate());

            given(esgChallengeRepository.findByUserIdAndStartDateAndStatus(
                    userId, expectedMonday, EsgChallengeStatus.IN_PROGRESS))
                    .willReturn(Optional.empty()); // IN_PROGRESS가 없으므로 empty

            // When
            listener.onCardPayment(event);

            // Then — 추가 상태 변경 없음 (verify no unexpected interactions)
            verifyNoMoreInteractions(esgChallengeRepository);
        }
    }
}
