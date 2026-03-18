package com.akku.backend.domain.quiz.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * FastAPI가 {@code quiz.chat.response} 토픽에 발행하는 CHAT_RESPONSE 이벤트 페이로드.
 *
 * <p><b>계약(Contract):</b>
 * FastAPI는 Stateful하게 전체 대화 기록을 관리하므로 {@code chatJson}은 반드시 non-null,
 * non-blank 값이어야 한다. null/blank 수신 시 컨슈머는 DB 업데이트를 완전히 중단한다.</p>
 */
public record ChatResponseEvent(

        @JsonProperty("event_type")
        String eventType,

        @JsonProperty("event_id")
        UUID eventId,

        @JsonProperty("user_id")
        UUID userId,

        @JsonProperty("quiz_id")
        UUID quizId,

        /** AI가 생성한 응답 텍스트 */
        String reply,

        /**
         * FastAPI가 Stateful하게 누적·직렬화한 전체 대화 기록 JSON.
         * null/blank 수신은 FastAPI 계약 위반이며, 컨슈머에서 DB 업데이트 차단 트리거가 된다.
         */
        @JsonProperty("chat_json")
        String chatJson
) {}
