package com.akku.backend.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 금융 퀴즈 문제 은행 엔티티 (quizzes)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String topic;

    @Column(nullable = false, length = 10)
    private String difficulty;

    /**
     * 퀴즈 내용 (문제 + 선지) — PostgreSQL JSONB 컬럼
     * Hibernate 6 네이티브 @JdbcTypeCode(SqlTypes.JSON) 사용
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "problem_json", nullable = false, columnDefinition = "jsonb")
    private String problemJson;

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
