package com.akku.backend.domain.bank.event;
 
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.entity.JellingTransaction;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.domain.jelling.repository.JellingTransactionRepository;
import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.akku.backend.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 카드 결제 시 5% 젤링 캐시백 처리를 담당하는 리스너.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JellingCardPaymentEventListener {

    private final JellingRepository jellingRepository;
    private final JellingTransactionRepository jellingTransactionRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCardPaymentCashback(CardPaymentEvent event) {
        log.debug("카드 결제 캐시백 처리 시작 — userId: {}, amount: {}", event.userId(), event.paymentBalance());

        // 5% 캐시백 금액 계산
        long cashbackAmount = (long) (event.paymentBalance() * 0.05);
        if (cashbackAmount <= 0) {
            return;
        }

        // 젤링 적립 (단순 조회 및 업데이트)
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;

        Jelling jelling = jellingRepository.findById(event.userId())
                .orElseGet(() -> jellingRepository.save(Jelling.builder()
                        .user(user)
                        .balance(0L)
                        .build()));

        jelling.addBalance(cashbackAmount);
        jellingRepository.save(jelling);

        // 젤링 변동 내역 기록
        jellingTransactionRepository.save(JellingTransaction.builder()
                .user(user)
                .amount(cashbackAmount)
                .type("REWARD")
                .description("카드 결제 캐시백 (5%)")
                .build());

        // FCM 알림 발송
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            try {
                fcmService.sendMessage(NotificationRequest.builder()
                        .token(user.getFcmToken())
                        .title("젤링이 도착했어요! \uD83C\uDF6C")
                        .body(String.format("카드 결제 보상으로 젤링 %,d개가 적립되었습니다.", cashbackAmount))
                        .build());
            } catch (Exception e) {
                log.warn("젤링 알림 발송 실패: {}", e.getMessage());
            }
        }

        log.info("카드 결제 캐시백 적립 완료 — userId: {}, cashback: {} 🍬", event.userId(), cashbackAmount);
    }
}
