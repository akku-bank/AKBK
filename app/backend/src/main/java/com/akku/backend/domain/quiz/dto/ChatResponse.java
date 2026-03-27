package com.akku.backend.domain.quiz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(
        @JsonProperty("ai_reply")
        String reply,
        @JsonProperty("deducted_credits")
        int deductedCredits,
        @JsonProperty("chat_json")
        String chatJson
) {}
