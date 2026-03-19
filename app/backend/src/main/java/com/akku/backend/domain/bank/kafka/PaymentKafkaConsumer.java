package com.akku.backend.domain.bank.kafka;

import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.event.PaymentEvent;
import com.akku.backend.domain.bank.event.TransactionCompletedEvent;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * {@code payment} 토픽을 소비하는 컨슈머.
 *
 * <h3>처리 순서</h3>
 * <ol>
 *   <li>raw JSON String → {@link PaymentEvent} 역직렬화</li>
 *   <li>eventId 기반 중복 체크 (이미 처리된 이벤트 skip)</li>
 *   <li>거래내역 DB 저장 (@Transactional)</li>
 *   <li>TransactionCompletedEvent 발행 → transaction 토픽</li>
 *   <li>ack.acknowledge() — 오프셋 수동 커밋</li>
 * </ol>
 *
 * <h3>오류 처리</h3>
 * <p>역직렬화 실패 또는 처리 중 예외 발생 시 로그를 남기고 ack 를 호출해
 * poison pill 로 인한 파티션 블록을 방지한다.
 * 향후 Dead Letter Topic(DLT) 연동으로 교체 가능.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionKafkaProducer transactionKafkaProducer;

    @KafkaListener(
            topics  = "${akku.kafka.topic.payment:payment}",
            groupId = "${akku.kafka.consumer.group.payment-storage:payment-storage-group}"
    )
    @Transactional
    public void handle(String payload, Acknowledgment ack) {
        PaymentEvent event = null;
        try {
            event = objectMapper.readValue(payload, PaymentEvent.class);

            // ── 중복 방지 ──────────────────────────────────────────────────────
            if (transactionRepository.existsByEventId(event.eventId())) {
                log.warn("PAYMENT 이벤트 중복 수신, skip - eventId: {}", event.eventId());
                return;
            }

            // ── DB 저장 ────────────────────────────────────────────────────────
            Transaction saved = transactionRepository.save(Transaction.fromPayment(event));
            log.info("PAYMENT 거래 저장 완료 - eventId: {}, userId: {}, amount: {}",
                    event.eventId(), event.userId(), event.amount());

            // ── transaction 토픽 발행 ──────────────────────────────────────────
            TransactionCompletedEvent completed =
                    TransactionCompletedEvent.fromPayment(saved, event,
                            saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now());
            transactionKafkaProducer.publish(completed);

        } catch (Exception e) {
            log.error("PAYMENT 이벤트 처리 실패 - payload: {}", payload, e);
        } finally {
            ack.acknowledge();
        }
    }
}
