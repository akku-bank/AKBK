package com.akku.backend.domain.quiz.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kafka 채팅 요청 이벤트 페이로드.
 * FastAPI 컨슈머가 수신하는 CHAT_REQUEST 이벤트와 JSON 계약이 정확히 일치해야 한다.
 */
public record QuizChatEvent(

        @JsonProperty("event_type")
        String eventType,

        @JsonProperty("event_id")
        UUID eventId,

        @JsonProperty("user_id")
        UUID userId,

        @JsonProperty("quiz_id")
        UUID quizId,

        String message,

        @JsonProperty("remaining_credits")
        int remainingCredits,

        String difficulty,

        @JsonProperty("birth_date")
        LocalDate birthDate
) {
    /**
     * event_type = CHAT_REQUEST 고정, event_id 자동 생성 팩토리 메서드.
     */
    public static QuizChatEvent of(UUID userId, UUID quizId, String message,
                                   int remainingCredits, String difficulty, LocalDate birthDate) {
        return new QuizChatEvent(
                "CHAT_REQUEST",
                UUID.randomUUID(),
                userId,
                quizId,
                message,
                remainingCredits,
                difficulty,
                birthDate
        );
    }
}
