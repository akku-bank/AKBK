package com.akku.backend.domain.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * AI 챗봇 힌트 요청 DTO
 */
public record ChatRequest(
        @NotNull UUID quizId,
        @NotBlank @Size(max = 500) String message   // 사용자 질문 메시지
) {}
