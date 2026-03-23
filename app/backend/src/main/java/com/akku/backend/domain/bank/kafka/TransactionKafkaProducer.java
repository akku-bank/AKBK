package com.akku.backend.domain.bank.kafka;

import com.akku.backend.domain.bank.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * 거래 완료 이벤트를 {@code transaction} 토픽에 발행하는 프로듀서.
 *
 * <h3>파티션 키 전략</h3>
 * <p>파티션 키로 {@code userId} 를 사용해 동일 사용자의 이벤트를 동일 파티션으로 라우팅한다.
 * 이로써 TransactionNotificationConsumer / TransactionJellyConsumer 가
 * 같은 사용자의 이벤트를 순서대로 처리할 수 있다.</p>
 *
 * <h3>동기 발행</h3>
 * <p>{@code kafkaTemplate.send(...).get()} 으로 브로커 응답을 기다린다.
 * 발행 실패 시 {@link RuntimeException}을 throw하여 caller(Consumer)가
 * ack 없이 예외를 전파할 수 있게 한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionKafkaProducer {

    @Value("${akku.kafka.topic.transaction:transaction}")
    private String transactionTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * {@link TransactionCompletedEvent}를 {@code transaction} 토픽에 동기 발행한다.
     *
     * @param event 거래 완료 이벤트 (Spark 규격 준수)
     * @throws RuntimeException 브로커 발행 실패 또는 스레드 인터럽트 시
     */
    public void publish(TransactionCompletedEvent event) {
        String partitionKey = event.data().userId();  // userId 기준 파티션 고정
        try {
            var result = kafkaTemplate.send(transactionTopic, partitionKey, event).get();
            log.info("transaction 이벤트 발행 완료 - eventId: {}, offset: {}",
                    event.data().id(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "transaction 이벤트 발행 중단 - eventId: " + event.data().id(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "transaction 이벤트 발행 실패 - eventId: " + event.data().id(), e);
        }
    }
}
