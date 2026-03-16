package com.akku.backend.domain.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 정답 제출 요청 DTO
 */
public record AnswerRequest(
        @NotNull UUID quizId,
        @Min(0) @Max(3) int selectedAnswer   // 사용자가 선택한 선지 번호 (0-3)
) {}
