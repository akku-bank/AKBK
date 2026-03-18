package com.akku.backend.domain.quiz.repository;

import com.akku.backend.domain.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * quizzes 테이블 레포지토리
 */
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    /**
     * 특정 난이도 + 생성 시각 범위 내 퀴즈 1건 조회
     * (어제 22:00 ~ 오늘 00:00 사이 생성된 당일 퀴즈를 찾는 데 사용)
     */
    Optional<Quiz> findTopByDifficultyAndCreatedAtBetween(
            String difficulty, LocalDateTime from, LocalDateTime to);
}
