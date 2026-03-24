package com.akku.backend.domain.challenge.event;

import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * 카드 결제 이벤트 리스너 — 실시간 FAIL 판정 담당.
 *
 * 설계 원칙:
 *  - @TransactionalEventListener(AFTER_COMMIT): 결제 트랜잭션이 완전히 커밋된 이후에만 실행.
 *    결제 트랜잭션 롤백 시 이 리스너는 실행되지 않으므로 데이터 정합성이 보장됨.
 *  - @Async: 별도 스레드에서 실행하여 결제 API 응답 속도에 영향을 주지 않음.
 *  - @Transactional(REQUIRES_NEW): 결제 트랜잭션과 완전히 분리된 새 트랜잭션을 시작.
 *    챌린지 상태 업데이트 실패가 결제에 영향을 주지 않음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardPaymentEventListener {

    private final SpendingChallengeRepository spendingChallengeRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCardPayment(CardPaymentEvent event) {
        log.debug("CardPaymentEvent 수신 — userId: {}, category: {}, amount: {}",
                event.userId(), event.subCategoryName(), event.paymentBalance());

        // 해당 유저의 결제 카테고리와 일치하는 진행 중 챌린지 조회
        spendingChallengeRepository
                .findActiveChallenge(event.userId(), event.subCategoryName(), ChallengeStatus.IN_PROGRESS, event.transactionDate())
                .ifPresent(challenge -> evaluateAndFail(challenge));
    }

    /**
     * 챌린지 기간 내 누적 소비액을 계산하여 목표 초과 시 FAIL 처리.
     * calculateCurrentSpending은 transactions 테이블 기준으로 이미 커밋된 최신 데이터를 조회함.
     */
    private void evaluateAndFail(SpendingChallenge challenge) {
        LocalDateTime startDateTime = challenge.getStartDate().atStartOfDay();
        LocalDateTime endExclusive = challenge.getEndDate().plusDays(1).atStartOfDay();

        Long currentSpending = spendingChallengeRepository.calculateCurrentSpending(
                challenge.getUser().getId(),
                challenge.getSubCategoryName(),
                startDateTime,
                endExclusive
        );

        if (currentSpending > challenge.getTargetSpending()) {
            challenge.updateStatus(ChallengeStatus.FAIL);
            log.info("챌린지 FAIL 판정 — challengeId: {}, targetSpending: {}, currentSpending: {}",
                    challenge.getId(), challenge.getTargetSpending(), currentSpending);
        }
    }
}
