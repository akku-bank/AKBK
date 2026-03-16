package com.akku.backend.domain.quiz.dto;

import java.util.UUID;

/**
 * 퀴즈 조회 응답 DTO
 */
public record QuizResponse(
        UUID quizId,
        String topic,
        String difficulty,
        String problemJson,
        String explanation,
        int remainingCredits,
        String chatJson // 금일 채팅 내역 포함
) {}
