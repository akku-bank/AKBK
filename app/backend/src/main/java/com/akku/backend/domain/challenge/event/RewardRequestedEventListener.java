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

import java.util.List;

/**
 * 보상 요청 이벤트 리스너 — 부모 FCM 푸시 알림 발송 담당.
 *
 * 설계 원칙:
 *  - @TransactionalEventListener(AFTER_COMMIT): 챌린지 상태가 REWARD_REQUESTED로 커밋된 후 실행.
 *  - @Async: FCM 외부 통신이 클라이언트 응답 속도에 영향을 주지 않도록 별도 스레드 처리.
 *  - FCM 발송은 부가 기능이므로, 발송 실패 시 예외를 격리(개별 try-catch)하여
 *    부모가 2명일 때 한 명 발송 실패가 나머지 발송을 중단시키지 않도록 보장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardRequestedEventListener {

    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onRewardRequested(RewardRequestedEvent event) {
        log.debug("RewardRequestedEvent 수신 — challengeId: {}, familyId: {}", event.challengeId(), event.familyId());

        // 같은 가족의 부모 전체 조회 (엄마+아빠 동시 알림 지원)
        List<User> parents = userRepository.findAllByFamilyIdAndRole(event.familyId(), "PARENT");

        if (parents.isEmpty()) {
            log.warn("FCM 발송 대상 부모 없음 — familyId: {}", event.familyId());
            return;
        }

        for (User parent : parents) {
            if (parent.getFcmToken() == null || parent.getFcmToken().isBlank()) {
                log.warn("FCM 토큰 미등록 — parentId: {}", parent.getId());
                continue;
            }

            try {
                fcmService.sendMessage(NotificationRequest.builder()
                        .token(parent.getFcmToken())
                        .title("보상 요청이 도착했어요!")
                        .body(String.format("%s(이)가 '%s' 챌린지 성공으로 %,d원 보상을 요청했습니다.",
                                event.childName(), event.category(), event.rewardAmount()))
                        .build());

                log.info("FCM 발송 성공 — parentId: {}, challengeId: {}", parent.getId(), event.challengeId());
            } catch (Exception e) {
                // 개별 부모 FCM 발송 실패를 격리 — 나머지 부모 발송은 계속 진행
                log.error("FCM 발송 실패 — parentId: {}, challengeId: {}", parent.getId(), event.challengeId(), e);
            }
        }
    }
}
