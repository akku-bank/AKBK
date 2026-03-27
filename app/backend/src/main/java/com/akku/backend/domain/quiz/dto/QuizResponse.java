package com.akku.backend.domain.quiz.dto;

import java.util.UUID;

public record QuizResponse(
        UUID quizId,
        String topic,
        String difficulty,
        String problemJson,
        String explanation,
        int remainingCredits,
        String chatJson,
        boolean isSubmitted,
        Boolean isCorrect
) {}
