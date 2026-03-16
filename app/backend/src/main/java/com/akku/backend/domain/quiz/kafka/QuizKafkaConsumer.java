package com.akku.backend.domain.quiz.kafka;

import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.event.ChatResponseEvent;
import com.akku.backend.domain.quiz.service.QuizService;
import com.akku.backend.domain.quiz.sse.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * FastAPI AI 서버의 CHAT_RESPONSE 이벤트를 소비하는 Kafka 컨슈머.
 *
 * <h3>처리 순서</h3>
 * <ol>
 *   <li>chatJson null/blank 가드 — FastAPI 계약 위반 감지 시 DB 업데이트 완전 차단</li>
 *   <li>DB UPSERT — {@link QuizService#upsertChatLog}의 {@code @Transactional} 경계 내에서 실행.
 *       이 메서드가 반환된 시점(= 트랜잭션 커밋 완료)에만 SSE 푸시를 진행한다.</li>
 *   <li>SSE 푸시 — 클라이언트에게 AI 응답 전달</li>
 *   <li>SSE 종료 — 스트림을 정상 완료 처리</li>
 * </ol>
 *
 * <p><b>트랜잭션 설계 의도:</b> 이 리스너 메서드 자체는 {@code @Transactional}이 아니다.
 * DB 쓰기는 {@code quizService.upsertChatLog()}의 독립적인 트랜잭션에 위임함으로써,
 * 트랜잭션 커밋 이후에 SSE 전송이 실행됨을 보장한다.
 * (트랜잭션 내부에서 SSE를 전송하면 커밋 전 클라이언트가 응답을 받는 경쟁 조건이 발생할 수 있다.)</p>
 *
 * <p>TODO: application.yml에 아래 Kafka Consumer 설정 필요:
 * <pre>
 * spring:
 *   kafka:
 *     consumer:
 *       group-id: ${akku.kafka.consumer.group-id:quiz-chat-group}
 *       auto-offset-reset: latest
 *       key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 *       value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
 *       properties:
 *         spring.json.trusted.packages: "com.akku.backend.domain.quiz.event"
 *         spring.json.value.default.type: "com.akku.backend.domain.quiz.event.ChatResponseEvent"
 * </pre>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizKafkaConsumer {

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
            topics    = "${akku.kafka.topic.quiz-chat-response:quiz.chat.response}",
            groupId   = "${akku.kafka.consumer.group-id:quiz-chat-group}"
    )
    public void handleChatResponse(ChatResponseEvent event) {
        log.info("CHAT_RESPONSE 수신 - eventId: {}, userId: {}, quizId: {}",
                event.eventId(), event.userId(), event.quizId());

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

        // ── 1. DB UPSERT ─────────────────────────────────────────────────────
        // quizService.upsertChatLog()의 @Transactional 경계 내에서 실행된다.
        // 이 호출이 반환된 시점 = 트랜잭션 커밋 완료. 이후에만 SSE 전송을 진행한다.
        quizService.upsertChatLog(event.userId(), event.quizId(), event.chatJson());
        log.debug("채팅 로그 UPSERT 완료 - userId: {}, quizId: {}", event.userId(), event.quizId());

        // ── 2. SSE 푸시 ───────────────────────────────────────────────────────
        // 재사용 기존 ChatResponse DTO: reply(AI 텍스트) + chatJson(전체 누적 기록)
        sseConnectionManager.send(
                event.userId(),
                new ChatResponse(event.reply(), event.chatJson()),
                "chat-response"
        );

        // ── 3. SSE 스트림 정상 종료 ───────────────────────────────────────────
        sseConnectionManager.complete(event.userId());
    }
}
