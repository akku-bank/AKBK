package com.akku.backend.domain.challenge.event;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.akku.backend.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 보상 송금 완료 이벤트 리스너 — 자녀 FCM 푸시 알림 발송 담당.
 *
 * 설계 원칙:
 *  - @TransactionalEventListener(AFTER_COMMIT): 챌린지 상태가 REWARDED로 커밋된 후 실행.
 *    커밋 전 실행 시 자녀가 알림을 받았으나 실제로는 상태가 롤백될 수 있는 문제를 방지.
 *  - @Async: FCM 외부 통신이 부모 API 응답 속도에 영향을 주지 않도록 별도 스레드 처리.
 *  - @Transactional(REQUIRES_NEW): AFTER_COMMIT 콜백은 원본 트랜잭션이 닫힌 뒤이므로
 *    자녀 조회를 위한 새 트랜잭션을 열어야 LazyInitializationException을 방지할 수 있다.
 *  - FCM 발송 실패는 try-catch로 격리 — 알림 실패가 핵심 결제 트랜잭션에 영향 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardTransferredEventListener {

    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onRewardTransferred(RewardTransferredEvent event) {
        log.debug("RewardTransferredEvent 수신 — challengeId: {}, childId: {}",
                event.challengeId(), event.childId());

        User child = userRepository.findById(event.childId()).orElse(null);

        if (child == null) {
            log.warn("FCM 발송 대상 자녀 없음 — childId: {}", event.childId());
            return;
        }

        if (child.getFcmToken() == null || child.getFcmToken().isBlank()) {
            log.warn("FCM 토큰 미등록 — childId: {}", child.getId());
            return;
        }

        try {
            fcmService.sendMessage(NotificationRequest.builder()
                    .token(child.getFcmToken())
                    .title("보상이 도착했어요! \uD83C\uDF89")
                    .body(String.format("'%s' 챌린지 보상으로 %,d원이 입금되었어요!",
                            event.category(), event.rewardAmount()))
                    .build());

            log.info("FCM 발송 성공 — childId: {}, challengeId: {}", child.getId(), event.challengeId());
        } catch (Exception e) {
            // FCM 발송 실패는 알림 부가 기능 실패이므로 예외를 격리하여 상위로 전파하지 않는다
            log.error("FCM 발송 실패 — childId: {}, challengeId: {}", child.getId(), event.challengeId(), e);
        }
    }
}
