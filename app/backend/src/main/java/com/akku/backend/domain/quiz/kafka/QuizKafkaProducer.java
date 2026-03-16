package com.akku.backend.domain.quiz.kafka;

import com.akku.backend.domain.quiz.event.QuizChatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 퀴즈 도메인 Kafka 프로듀서.
 *
 * <p>토픽명은 application.yml의 akku.kafka.topic.quiz-chat-request로 관리한다.</p>
 *
 * TODO: KafkaTemplate config — application.yml에 아래 설정 필요:
 *   spring.kafka.bootstrap-servers=...
 *   spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
 *   spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
 *   spring.kafka.producer.properties.spring.json.add.type.headers=false
 *   (LocalDate 직렬화) spring.jackson.serialization.write-dates-as-timestamps=false
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizKafkaProducer {

    @Value("${akku.kafka.topic.quiz-chat-request:quiz.chat.request}")
    private String quizChatRequestTopic;

    // TODO: KafkaTemplate config — JsonSerializer 기반 ProducerFactory 빈 등록 필요
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * CHAT_REQUEST 이벤트를 Kafka에 발행한다.
     * 파티션 키로 userId를 사용해 동일 사용자의 메시지 순서를 보장한다.
     */
    public void publishChatRequest(QuizChatEvent event) {
        kafkaTemplate.send(quizChatRequestTopic, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka CHAT_REQUEST 발행 실패 - eventId: {}", event.eventId(), ex);
                        log.debug("Kafka CHAT_REQUEST 발행 실패 상세 - userId: {}, quizId: {}",
                                event.userId(), event.quizId());
                    } else {
                        log.info("Kafka CHAT_REQUEST 발행 완료 - eventId: {}, offset: {}",
                                event.eventId(), result.getRecordMetadata().offset());
                        log.debug("Kafka CHAT_REQUEST 발행 완료 상세 - userId: {}, quizId: {}",
                                event.userId(), event.quizId());
                    }
                });
    }
}
