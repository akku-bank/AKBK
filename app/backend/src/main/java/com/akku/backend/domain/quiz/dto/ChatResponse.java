package com.akku.backend.domain.quiz.dto;

/**
 * FastAPI AI 서버 힌트 응답 DTO
 */
public record ChatResponse(
        String reply,       // AI 답변 텍스트
        String chatJson     // FastAPI가 직렬화하여 반환하는 전체 대화 기록 JSON
) {}
