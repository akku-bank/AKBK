package com.akku.backend.domain.bank.kafka;

import com.akku.backend.domain.bank.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 거래 완료 이벤트를 {@code transaction} 토픽에 발행하는 프로듀서.
 *
 * <h3>파티션 키 전략</h3>
 * <p>파티션 키로 {@code userId} 를 사용해 동일 사용자의 이벤트를 동일 파티션으로 라우팅한다.
 * 이로써 TransactionNotificationConsumer / TransactionJellyConsumer 가
 * 같은 사용자의 이벤트를 순서대로 처리할 수 있다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionKafkaProducer {

    @Value("${akku.kafka.topic.transaction:transaction}")
    private String transactionTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * {@link TransactionCompletedEvent}를 {@code transaction} 토픽에 발행한다.
     *
     * @param event 거래 완료 이벤트 (Spark 규격 준수)
     */
    public void publish(TransactionCompletedEvent event) {
        String partitionKey = event.data().userId();  // userId 기준 파티션 고정

        kafkaTemplate.send(transactionTopic, partitionKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("transaction 이벤트 발행 실패 - eventId: {}, userId: {}",
                                event.data().id(), event.data().userId(), ex);
                    } else {
                        log.info("transaction 이벤트 발행 완료 - eventId: {}, offset: {}",
                                event.data().id(), result.getRecordMetadata().offset());
                    }
                });
    }
}
