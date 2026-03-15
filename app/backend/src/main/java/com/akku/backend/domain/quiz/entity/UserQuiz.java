package com.akku.backend.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 유저 퀴즈 풀이 이력 엔티티 (user_quizzes)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@IdClass(UserQuizId.class)
@Table(name = "user_quizzes")
public class UserQuiz {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "remaining_credits", nullable = false)
    private int remainingCredits;

    @Column(name = "is_submitted", nullable = false)
    private boolean isSubmitted;

    /**
     * 정답 여부 — 아직 제출 전이면 null
     */
    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "solved_date")
    private LocalDate solvedDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserQuiz(UUID userId, UUID quizId, int remainingCredits) {
        this.userId = userId;
        this.quizId = quizId;
        this.remainingCredits = remainingCredits;
        this.isSubmitted = false;
    }

    /**
     * 정답 제출 시 결과를 반영
     */
    public void submit(boolean isCorrect) {
        this.isSubmitted = true;
        this.isCorrect = isCorrect;
        this.solvedDate = LocalDate.now();
    }
}
