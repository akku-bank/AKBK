package com.akku.backend.domain.quiz.dto;

/**
 * 정답 제출 응답 DTO
 */
public record AnswerResponse(
        boolean isCorrect,
        Long jellingReward,  // 정답 시 지급된 젤링 양, 오답이면 null
        int correctChoiceNo  // 실제 정답 번호 (1-4)
) {}
