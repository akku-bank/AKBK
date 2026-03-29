package com.akku.backend.domain.notification.event;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.bank.event.DepositReceivedEvent;
import com.akku.backend.domain.bank.event.RemittanceEvent;
import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.akku.backend.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionNotificationListener {

    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCardPayment(CardPaymentEvent event) {
        log.info("카드 결제 알림 처리 — userId: {}, amount: {}", event.userId(), event.paymentBalance());
        
        sendNotification(event.userId(), 
                "결제 완료! \uD83D\uDCC3", 
                String.format("%s에서 %,d원이 결제되었습니다.", event.merchantName(), event.paymentBalance()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRemittance(RemittanceEvent event) {
        log.info("송금 알림 처리 — userId: {}, target: {}, amount: {}", event.userId(), event.targetName(), event.amount());

        sendNotification(event.userId(),
                "송금 완료! \uD83D\uDCA3",
                String.format("%s님께 %,d원을 보냈습니다.", event.targetName(), event.amount()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepositReceived(DepositReceivedEvent event) {
        log.info("입금 알림 처리 — userId: {}, sender: {}, amount: {}", event.userId(), event.senderName(), event.amount());

        sendNotification(event.userId(),
                "입금 완료! \uD83D\uDCB0",
                String.format("%s님으로부터 %,d원이 입금되었습니다.", event.senderName(), event.amount()));
    }

    private void sendNotification(java.util.UUID userId, String title, String body) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            log.warn("FCM 알림 스킵 (토큰 없음) — userId: {}", userId);
            return;
        }

        try {
            fcmService.sendMessage(NotificationRequest.builder()
                    .token(user.getFcmToken())
                    .title(title)
                    .body(body)
                    .build());
            log.info("FCM 알림 발송 성공 — userId: {}, title: {}", userId, title);
        } catch (Exception e) {
            log.error("FCM 알림 발송 실패 — userId: {}", userId, e);
        }
    }
}
