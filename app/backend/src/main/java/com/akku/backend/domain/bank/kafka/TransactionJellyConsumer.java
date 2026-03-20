package com.akku.backend.domain.bank.kafka;

import com.akku.backend.domain.bank.event.TransactionCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * {@code transaction} 토픽을 소비하여 젤링(Jelly) 리워드를 계산하는 컨슈머.
 *
 * <h3>처리 조건</h3>
 * <ul>
 *   <li>{@code event_source = "PAYMENT"} 인 경우만 처리한다.</li>
 *   <li>{@code is_jelly_target = true} 인 경우만 젤링 계산을 수행한다.</li>
 * </ul>
 *
 * <h3>ack 정책 요약</h3>
 * <ul>
 *   <li><b>역직렬화 실패</b>: 재시도 불가(poison-pill) → ack 후 return</li>
 *   <li><b>eventSource != PAYMENT</b>: 정상 메시지, 처리 대상 아님 → ack 후 return</li>
 *   <li><b>isJellyTarget != true</b>: 정상 메시지, 젤링 계산 불필요 → ack 후 return</li>
 *   <li><b>젤링 계산 실패</b>: 재시도 필요 → ack 하지 않고 예외 전파 (Kafka 재소비 또는 DLQ)</li>
 *   <li><b>성공</b>: 젤링 계산 완료 후 ack</li>
 * </ul>
 *
 * <h3>TODO</h3>
 * <p>젤링 계산 로직은 별도 JellyService 로 분리하여 주입한다.
 * 현재는 placeholder 로그로 대체한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionJellyConsumer {

    private final ObjectMapper objectMapper;
    // TODO: private final JellyService jellyService;

    @KafkaListener(
            topics  = "${akku.kafka.topic.transaction:transaction}",
            groupId = "${akku.kafka.consumer.group.transaction-jelly:transaction-jelly-group}"
    )
    public void handle(String payload, Acknowledgment ack) {
        // ── [1] 역직렬화 실패: 재시도해도 동일하게 실패하므로 ack 후 버린다 ──────────
        TransactionCompletedEvent event;
        try {
            event = objectMapper.readValue(payload, TransactionCompletedEvent.class);
        } catch (Exception e) {
            log.error("TransactionCompletedEvent 역직렬화 실패 - payload: {}", payload, e);
            ack.acknowledge();
            return;
        }

        TransactionCompletedEvent.Data data = event.data();

        // ── [2] 처리 대상 필터: 정상 메시지이나 skip 대상 → ack 후 return ──────────
        if (!"PAYMENT".equals(data.eventSource())) {
            log.debug("젤링 계산 대상 아님 (PAYMENT 아님), skip - eventId: {}, eventSource: {}",
                    data.id(), data.eventSource());
            ack.acknowledge();
            return;
        }
        if (!Boolean.TRUE.equals(data.isJellyTarget())) {
            log.debug("젤링 대상 아님, skip - eventId: {}, isJellyTarget: {}",
                    data.id(), data.isJellyTarget());
            ack.acknowledge();
            return;
        }

        // ── [3] 젤링 계산: 실패 시 ack 없이 예외 전파 → Kafka 재소비 또는 DLQ ──────
        log.info("젤링 계산 대상 - eventId: {}, userId: {}, amount: {}, subCategory: {}",
                data.id(), data.userId(), data.amount(), data.subCategoryName());
        try {
            // TODO: jellyService.calculateAndSave(data.userId(), data.amount(), data.subCategoryId());
        } catch (Exception e) {
            log.error("젤링 계산 실패 — ack 없이 예외 전파. eventId={}, userId={}, amount={}, subCategoryId={}",
                    data.id(), data.userId(), data.amount(), data.subCategoryId(), e);
            throw e;
        }

        // ── [4] 젤링 계산 완료 → 오프셋 커밋 ────────────────────────────────────
        ack.acknowledge();
    }
}
