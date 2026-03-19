package com.akku.backend.global.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka 토픽명 / 컨슈머 그룹 ID 프로퍼티.
 *
 * <p>application.yml의 {@code akku.kafka.*} 아래에서 값을 읽는다.
 * 토픽명과 그룹 ID를 한 곳에서 관리해 오타·중복을 방지한다.</p>
 *
 * <pre>
 * akku:
 *   kafka:
 *     topic:
 *       quiz-chat-request: quiz.chat.request
 *       payment: payment
 *       ...
 *     consumer:
 *       group:
 *         quiz-chat: quiz-chat-group
 *         payment-storage: payment-storage-group
 *         ...
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "akku.kafka")
public class KafkaTopicProperties {

    private Topic topic = new Topic();
    private Consumer consumer = new Consumer();

    @Getter
    @Setter
    public static class Topic {
        private String quizChatRequest  = "quiz.chat.request";
        private String quizChatResponse = "quiz.chat.response";
        private String payment          = "payment";
        private String transfer         = "transfer";
        private String deposit          = "deposit";
        private String transaction      = "transaction";
    }

    @Getter
    @Setter
    public static class Consumer {
        private Group group = new Group();

        @Getter
        @Setter
        public static class Group {
            private String quizChat              = "quiz-chat-group";
            private String paymentStorage        = "payment-storage-group";
            private String transferStorage       = "transfer-storage-group";
            private String depositStorage        = "deposit-storage-group";
            private String transactionNotification = "transaction-notification-group";
            private String transactionJelly      = "transaction-jelly-group";
        }
    }
}
