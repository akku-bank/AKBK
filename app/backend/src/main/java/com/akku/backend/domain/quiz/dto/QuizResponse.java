package com.akku.backend.domain.quiz.dto;

import java.util.UUID;

/**
 * 퀴즈 조회 응답 DTO
 */
public record QuizResponse(
        UUID quizId,
        String topic,
        String difficulty,
        String problemJson,   // JSONB 원본 String 그대로 반환 (프론트가 파싱)
        String explanation,
        int remainingCredits
) {}
