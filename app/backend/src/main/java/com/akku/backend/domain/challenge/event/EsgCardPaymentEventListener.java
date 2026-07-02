package com.akku.backend.domain.challenge.event;

import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.challenge.entity.EsgChallengeStatus;
import com.akku.backend.domain.challenge.repository.EsgChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * ESG 챌린지 전용 카드 결제 이벤트 리스너 — 녹색 가맹점 결제 시 SUCCESS 판정 담당.
 *
 * 설계 원칙:
 *  - @EventListener: CardService.afterCommit()에서 이미 결제 트랜잭션 커밋 후 이벤트가 발행되므로
 *    @TransactionalEventListener 불필요. (afterCommit() 내부에서 발행된 이벤트는
 *    @TransactionalEventListener(AFTER_COMMIT)에 의해 처리되지 않음)
 *  - @Async: 별도 스레드에서 실행하여 결제 API 응답 속도에 영향을 주지 않음.
 *  - @Transactional(REQUIRES_NEW): 챌린지 상태 업데이트 실패가 결제에 영향을 주지 않음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsgCardPaymentEventListener {

    private final EsgChallengeRepository esgChallengeRepository;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCardPayment(CardPaymentEvent event) {
        // 녹색 가맹점이 아니면 즉시 반환 (early exit)
        if (!event.isGreen()) {
            return;
        }

        log.debug("녹색 가맹점 결제 감지 — userId: {}, amount: {}", event.userId(), event.paymentBalance());

        LocalDate thisMonday = event.transactionDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        esgChallengeRepository
                .findByUserIdAndStartDateAndStatus(event.userId(), thisMonday, EsgChallengeStatus.IN_PROGRESS)
                .ifPresent(challenge -> {
                    challenge.markSuccess();
                    log.info("ESG 챌린지 SUCCESS 판정 — challengeId: {}, userId: {}",
                            challenge.getId(), event.userId());
                });
    }
}
