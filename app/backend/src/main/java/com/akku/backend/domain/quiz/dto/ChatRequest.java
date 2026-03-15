package com.akku.backend.domain.quiz.dto;

import java.util.UUID;

/**
 * AI 챗봇 힌트 요청 DTO
 */
public record ChatRequest(
        UUID quizId,
        String message   // 사용자 질문 메시지
) {}
