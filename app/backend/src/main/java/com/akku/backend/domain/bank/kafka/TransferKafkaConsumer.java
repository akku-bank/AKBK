package com.akku.backend.domain.bank.kafka;

import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.event.TransactionCompletedEvent;
import com.akku.backend.domain.bank.event.TransferEvent;
import com.akku.backend.domain.bank.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@code transfer} 토픽을 소비하는 컨슈머.
 *
 * <h3>처리 순서</h3>
 * <ol>
 *   <li>raw JSON String → {@link TransferEvent} 역직렬화</li>
 *   <li>역직렬화 실패 → ack 후 return (poison-pill)</li>
 *   <li>transaction_type != "SPEND" → ack 후 return (비정상 데이터)</li>
 *   <li>{@link TransactionService#saveTransfer} — DB 저장 및 중복 체크 (@Transactional)</li>
 *   <li>중복 이벤트 → ack 후 return (skip)</li>
 *   <li>DB 저장 실패 → 예외 전파 (ack 하지 않음, Kafka 재소비)</li>
 *   <li>TransactionCompletedEvent 동기 발행 → transaction 토픽</li>
 *   <li>Kafka 발행 실패 → 예외 전파 (ack 하지 않음, Kafka 재소비)</li>
 *   <li>모든 성공 시 ack.acknowledge()</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;
    private final TransactionKafkaProducer transactionKafkaProducer;

    @KafkaListener(
            topics  = "${akku.kafka.topic.transfer:transfer}",
            groupId = "${akku.kafka.consumer.group.transfer-storage:transfer-storage-group}"
    )
    public void handle(String payload, Acknowledgment ack) {
        // ── [1] 역직렬화 실패: 재시도해도 동일하게 실패하므로 ack 후 버린다 ──────────
        TransferEvent event;
        try {
            event = objectMapper.readValue(payload, TransferEvent.class);
        } catch (Exception e) {
            log.error("TRANSFER 역직렬화 실패 - payload: {}", payload, e);
            ack.acknowledge();
            return;
        }

        // ── [2] transaction_type 검증: TRANSFER는 반드시 SPEND ───────────────────
        if (!"SPEND".equals(event.transactionType())) {
            log.error("INVALID transaction_type for TRANSFER - eventId: {}, userId: {}, type: {}",
                    event.eventId(), event.userId(), event.transactionType());
            ack.acknowledge();
            return;
        }

        // ── [3] DB 저장: 실패 시 ack 없이 예외 전파 → Kafka 재소비 ────────────────
        Optional<Transaction> savedOpt;
        try {
            savedOpt = transactionService.saveTransfer(event);
        } catch (Exception e) {
            log.error("TRANSFER DB 저장 실패 — ack 없이 예외 전파. eventId={}, userId={}",
                    event.eventId(), event.userId(), e);
            throw e;
        }

        // ── [4] 중복 이벤트: 정상 메시지이므로 ack 후 return ─────────────────────
        if (savedOpt.isEmpty()) {
            ack.acknowledge();
            return;
        }
        Transaction saved = savedOpt.get();

        // ── [5] Kafka 발행: 실패 시 ack 없이 예외 전파 → Kafka 재소비 ─────────────
        TransactionCompletedEvent completed = TransactionCompletedEvent.fromTransfer(
                saved, event,
                saved.getCreatedAt() != null ? saved.getCreatedAt() : event.createdAt());
        try {
            transactionKafkaProducer.publish(completed);
        } catch (Exception e) {
            log.error("TRANSFER transaction 이벤트 발행 실패 — ack 없이 예외 전파. eventId={}, userId={}",
                    event.eventId(), event.userId(), e);
            throw e;
        }

        // ── [6] DB 커밋 + Kafka 발행 모두 성공 → 오프셋 커밋 ────────────────────
        ack.acknowledge();
    }
}
