package com.akku.backend.domain.quiz.dto;

/**
 * 정답 제출 응답 DTO
 */
public record AnswerResponse(
        boolean isCorrect,
        Long jellingReward   // 정답 시 지급된 젤링 양, 오답이면 null
) {}
