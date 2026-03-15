package com.akku.backend.domain.quiz.dto;

import java.util.UUID;

/**
 * 정답 제출 요청 DTO
 */
public record AnswerRequest(
        UUID quizId,
        int selectedAnswer   // 사용자가 선택한 선지 번호
) {}
