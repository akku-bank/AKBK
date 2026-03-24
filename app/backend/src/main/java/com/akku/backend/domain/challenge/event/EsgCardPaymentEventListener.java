package com.akku.backend.domain.challenge.event;

import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.challenge.entity.EsgChallengeStatus;
import com.akku.backend.domain.challenge.repository.EsgChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * ESG 챌린지 전용 카드 결제 이벤트 리스너 — 녹색 가맹점 결제 시 SUCCESS 판정 담당.
 *
 * 설계 원칙:
 *  - @TransactionalEventListener(AFTER_COMMIT): 결제 트랜잭션이 완전히 커밋된 이후에만 실행.
 *    결제 롤백 시 리스너가 실행되지 않으므로 데이터 정합성이 보장됨.
 *  - @Async: 별도 스레드에서 실행하여 결제 API 응답 속도에 영향을 주지 않음.
 *  - @Transactional(REQUIRES_NEW): 결제 트랜잭션과 완전히 분리된 새 트랜잭션.
 *    챌린지 상태 업데이트 실패가 결제에 영향을 주지 않음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsgCardPaymentEventListener {

    private final EsgChallengeRepository esgChallengeRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
