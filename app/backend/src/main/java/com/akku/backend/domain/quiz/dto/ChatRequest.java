package com.akku.backend.domain.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * AI 챗봇 힌트 요청 DTO
 */
public record ChatRequest(
        @NotNull UUID quizId,
        @NotBlank String message   // 사용자 질문 메시지
) {}
