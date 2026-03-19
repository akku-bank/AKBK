package com.akku.backend.domain.bank.kafka;

import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.event.DepositEvent;
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
 * {@code deposit} 토픽을 소비하는 컨슈머.
 *
 * <h3>처리 순서</h3>
 * <ol>
 *   <li>raw JSON String → {@link DepositEvent} 역직렬화</li>
 *   <li>eventId 기반 중복 체크</li>
 *   <li>거래내역 DB 저장 (@Transactional)</li>
 *   <li>TransactionCompletedEvent 발행 → transaction 토픽</li>
 *   <li>ack.acknowledge() — 오프셋 수동 커밋</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepositKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionKafkaProducer transactionKafkaProducer;

    @KafkaListener(
            topics  = "${akku.kafka.topic.deposit:deposit}",
            groupId = "${akku.kafka.consumer.group.deposit-storage:deposit-storage-group}"
    )
    @Transactional
    public void handle(String payload, Acknowledgment ack) {
        try {
            DepositEvent event = objectMapper.readValue(payload, DepositEvent.class);

            // ── 중복 방지 ──────────────────────────────────────────────────────
            if (transactionRepository.existsByEventId(event.eventId())) {
                log.warn("DEPOSIT 이벤트 중복 수신, skip - eventId: {}", event.eventId());
                return;
            }

            // ── DB 저장 ────────────────────────────────────────────────────────
            Transaction saved = transactionRepository.save(Transaction.fromDeposit(event));
            log.info("DEPOSIT 거래 저장 완료 - eventId: {}, userId: {}, amount: {}",
                    event.eventId(), event.userId(), event.amount());

            // ── transaction 토픽 발행 ──────────────────────────────────────────
            TransactionCompletedEvent completed =
                    TransactionCompletedEvent.fromDeposit(saved, event,
                            saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now());
            transactionKafkaProducer.publish(completed);

        } catch (Exception e) {
            log.error("DEPOSIT 이벤트 처리 실패 - payload: {}", payload, e);
        } finally {
            ack.acknowledge();
        }
    }
}
