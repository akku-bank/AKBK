package com.akku.backend.domain.quiz.kafka;

import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.event.ChatResponseEvent;
import com.akku.backend.domain.quiz.service.QuizService;
import com.akku.backend.domain.quiz.sse.SseConnectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * FastAPI AI 서버의 CHAT_RESPONSE 이벤트를 소비하는 Kafka 컨슈머.
 *
 * <h3>처리 순서</h3>
 * <ol>
 *   <li>raw JSON String → {@link ChatResponseEvent} 역직렬화 (ObjectMapper 직접 사용)</li>
 *   <li>chatJson null/blank 가드 — FastAPI 계약 위반 감지 시 DB 업데이트 완전 차단</li>
 *   <li>DB UPSERT — {@link QuizService#upsertChatLog}의 {@code @Transactional} 경계 내에서 실행.
 *       이 메서드가 반환된 시점(= 트랜잭션 커밋 완료)에만 SSE 푸시를 진행한다.</li>
 *   <li>SSE 푸시 — 클라이언트에게 AI 응답 전달</li>
 *   <li>SSE 종료 — 스트림을 정상 완료 처리</li>
 *   <li>ack.acknowledge() — 오프셋 수동 커밋</li>
 * </ol>
 *
 * <h3>역직렬화 방식 변경 이유</h3>
 * <p>기존에는 application.yml 의 {@code spring.json.value.default.type} 을 통해
 * 전역 Kafka Consumer 가 {@link ChatResponseEvent} 로 자동 역직렬화했다.
 * 이 방식은 bank 등 다른 도메인 이벤트를 추가할 때 전역 설정을 오염시킨다.
 * 개선 후에는 공통 ConsumerFactory 가 값을 {@code String} 으로 수신하고,
 * 각 컨슈머가 ObjectMapper 로 자신이 원하는 타입에 직접 역직렬화한다.</p>
 *
 * <p><b>트랜잭션 설계 의도:</b> 이 리스너 메서드 자체는 {@code @Transactional}이 아니다.
 * DB 쓰기는 {@code quizService.upsertChatLog()}의 독립적인 트랜잭션에 위임함으로써,
 * 트랜잭션 커밋 이후에 SSE 전송이 실행됨을 보장한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final QuizService quizService;
    private final SseConnectionManager sseConnectionManager;

    /**
     * {@code quiz.chat.response} 토픽의 CHAT_RESPONSE 이벤트를 처리한다.
     *
     * <p><b>[CRITICAL FALLBACK RULE]</b><br>
     * {@code chatJson}이 null 또는 blank인 경우, FastAPI의 Stateful 보장이 깨진 것으로 간주한다.
     * 이때 기존 {@code chat_logs} 데이터를 절대 덮어쓰지 않으며 DB 업데이트를 완전히 중단한다.
     * 사용자에게 reply 텍스트만 SSE로 전달하여 UX 단절을 최소화한다.</p>
     */
    @KafkaListener(
            topics  = "${akku.kafka.topic.quiz-chat-response:quiz.chat.response}",
            groupId = "${akku.kafka.consumer.group.quiz-chat:quiz-chat-group}"
    )
    public void handleChatResponse(String payload, Acknowledgment ack) {
        ChatResponseEvent event;
        try {
            event = objectMapper.readValue(payload, ChatResponseEvent.class);
        } catch (Exception e) {
            log.error("CHAT_RESPONSE 역직렬화 실패 - payload: {}", payload, e);
            ack.acknowledge();
            return;
        }

        log.info("CHAT_RESPONSE 수신 - eventId: {}, userId: {}, quizId: {}",
                event.eventId(), event.userId(), event.quizId());

        try {
            // ── [CRITICAL] chatJson null/blank 가드 ──────────────────────────────
            if (event.chatJson() == null || event.chatJson().isBlank()) {
                log.error(
                        "CRITICAL: chatJson null/blank — FastAPI Stateful 계약 위반. " +
                        "데이터 손실 방지를 위해 DB 업데이트를 완전히 차단합니다. " +
                        "eventId={}, userId={}, quizId={}",
                        event.eventId(), event.userId(), event.quizId()
                );
                // DB는 건드리지 않고, reply가 있을 경우에만 SSE로 텍스트 전달 (chatJson=null 명시)
                if (event.reply() != null && !event.reply().isBlank()) {
                    sseConnectionManager.send(
                            event.userId(),
                            new ChatResponse(event.reply(), null),
                            "chat-response"
                    );
                }
                sseConnectionManager.complete(event.userId());
                return;
            }

            // ── 1. DB UPSERT + 2. SSE 푸시 ───────────────────────────────────────
            // finally로 complete() 보장 — upsert/send 예외 시에도 SSE 연결이 hang되지 않음
            try {
                // quizService.upsertChatLog()의 @Transactional 경계 내에서 실행된다.
                // 이 호출이 반환된 시점 = 트랜잭션 커밋 완료. 이후에만 SSE 전송을 진행한다.
                quizService.upsertChatLog(event.userId(), event.quizId(), event.chatJson());
                log.debug("채팅 로그 UPSERT 완료 - userId: {}, quizId: {}", event.userId(), event.quizId());

                sseConnectionManager.send(
                        event.userId(),
                        new ChatResponse(event.reply(), event.chatJson()),
                        "chat-response"
                );
            } finally {
                // ── 3. SSE 스트림 정상 종료 ─────────────────────────────────────
                sseConnectionManager.complete(event.userId());
            }
        } finally {
            ack.acknowledge();
        }
    }
}
