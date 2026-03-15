package com.akku.backend.domain.quiz.repository;

import com.akku.backend.domain.quiz.entity.UserQuiz;
import com.akku.backend.domain.quiz.entity.UserQuizId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * user_quizzes 테이블 레포지토리
 */
public interface UserQuizRepository extends JpaRepository<UserQuiz, UserQuizId> {

    /**
     * 유저 ID + 퀴즈 ID로 풀이 이력 조회
     */
    Optional<UserQuiz> findByUserIdAndQuizId(UUID userId, UUID quizId);
}
