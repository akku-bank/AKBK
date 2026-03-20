package com.akku.backend.domain.bank.kafka;

import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.bank.event.TransactionCompletedEvent;
import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.akku.backend.domain.notification.service.FcmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * {@code transaction} 토픽을 소비하여 FCM 푸시 알림을 전송하는 컨슈머.
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>raw JSON String → {@link TransactionCompletedEvent} 역직렬화</li>
 *   <li>UserRepository 로 FCM 토큰 조회</li>
 *   <li>FcmService 로 푸시 알림 전송</li>
 *   <li>ack.acknowledge()</li>
 * </ol>
 *
 * <p>FCM 토큰이 없거나 발송 실패 시 로그만 남기고 ack 를 수행한다.
 * 알림 실패가 거래 파이프라인 전체를 블록하지 않도록 설계한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @KafkaListener(
            topics  = "${akku.kafka.topic.transaction:transaction}",
            groupId = "${akku.kafka.consumer.group.transaction-notification:transaction-notification-group}"
    )
    public void handle(String payload, Acknowledgment ack) {
        try {
            TransactionCompletedEvent event = objectMapper.readValue(payload, TransactionCompletedEvent.class);
            TransactionCompletedEvent.Data data = event.data();

            UUID userId = UUID.fromString(data.userId());

            userRepository.findById(userId).ifPresentOrElse(user -> {
                if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
                    log.debug("FCM 토큰 없음, 알림 skip - userId: {}", userId);
                    return;
                }

                String body = buildNotificationBody(data);

                NotificationRequest request = NotificationRequest.builder()
                        .token(user.getFcmToken())
                        .title("아꾸뱅꾸 거래 알림")
                        .body(body)
                        .build();

                fcmService.sendMessage(request);
                log.info("거래 알림 전송 완료 - eventId: {}, userId: {}", data.id(), userId);

            }, () -> log.warn("사용자 조회 실패, 알림 skip - userId: {}", userId));

        } catch (Exception e) {
            log.error("거래 알림 처리 실패 - payload: {}", payload, e);
        } finally {
            ack.acknowledge();
        }
    }

    private String buildNotificationBody(TransactionCompletedEvent.Data data) {
        String type = switch (data.transactionType()) {
            case "SPEND" -> "출금";
            case "INCOME" -> "입금";
            default -> "거래";
        };
        String source = switch (data.eventSource()) {
            case "PAYMENT"  -> "결제";
            case "TRANSFER" -> "이체";
            case "DEPOSIT"  -> "입금";
            default         -> "거래";
        };
        return String.format("%s %,d원 %s 완료", source, data.amount(), type);
    }
}
