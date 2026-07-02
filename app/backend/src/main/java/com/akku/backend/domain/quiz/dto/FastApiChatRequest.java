package com.akku.backend.domain.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record FastApiChatRequest(
        @JsonProperty("event_type")
        String eventType,
        @JsonProperty("event_id")
        UUID eventId,
        @JsonProperty("user_id")
        UUID userId,
        @JsonProperty("quiz_id")
        UUID quizId,
        @JsonProperty("message")
        String message,
        @JsonProperty("remaining_credits")
        int remainingCredits,
        @JsonProperty("difficulty")
        String difficulty,
        @JsonProperty("birth_date")
        LocalDate birthDate,
        @JsonProperty("chat_json")
        String chatJson
) {}
