package com.akku.backend.global.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 공통 인프라 설정.
 *
 * <h3>설계 원칙</h3>
 * <ul>
 *   <li>이 클래스는 Kafka 인프라(Factory, Template, ContainerFactory)만 담당한다.</li>
 *   <li>비즈니스 Producer / Consumer 빈은 각 도메인(bank, quiz) 안에 둔다.</li>
 *   <li>Consumer 값 역직렬화는 {@link StringDeserializer}를 사용해 raw JSON String으로 수신한다.
 *       각 도메인 컨슈머가 ObjectMapper로 타입 안전하게 역직렬화하므로
 *       전역 {@code spring.json.value.default.type} 설정이 불필요하다.</li>
 * </ul>
 *
 * <h3>Producer 설정</h3>
 * <ul>
 *   <li>acks=all + enable.idempotence=true → 정확히 한 번 발행 보장</li>
 *   <li>ADD_TYPE_INFO_HEADERS=false → FastAPI 등 외부 컨슈머와 호환성 유지</li>
 * </ul>
 *
 * <h3>Consumer 설정</h3>
 * <ul>
 *   <li>enable-auto-commit=false + AckMode.MANUAL → 컨슈머가 직접 오프셋 커밋 제어</li>
 * </ul>
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Producer ─────────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // 정확히 한 번 발행 보장: acks=all + idempotence
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);
        // 타입 헤더 미추가 → FastAPI 등 외부 시스템(Spark 등)과의 호환성 유지
        factory.setValueSerializer(new JsonSerializer<>(objectMapper).noTypeInfo());
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // ── Consumer ─────────────────────────────────────────────────────────────

    /**
     * 공통 ConsumerFactory.
     *
     * <p>값(value)을 {@link StringDeserializer}로 수신한다.
     * 각 도메인 컨슈머 메서드가 {@code String payload} 파라미터로 raw JSON을 받아
     * ObjectMapper로 원하는 타입에 직접 역직렬화한다.
     * 이 방식으로 전역 default.type 설정 없이 quiz / bank 등 여러 도메인 이벤트를 처리할 수 있다.</p>
     */
    @Bean
    public DefaultKafkaConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 공통 KafkaListenerContainerFactory.
     *
     * <p>AckMode.MANUAL → 컨슈머가 {@code ack.acknowledge()} 를 명시적으로 호출해야만 오프셋이 커밋된다.
     * 처리 실패 시 ack를 호출하지 않으면 메시지를 재소비할 수 있다.</p>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
