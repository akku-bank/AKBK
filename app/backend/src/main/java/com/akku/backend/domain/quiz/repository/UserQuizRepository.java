package com.akku.backend.domain.quiz.repository;

import com.akku.backend.domain.quiz.entity.UserQuiz;
import com.akku.backend.domain.quiz.entity.UserQuizId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /**
     * 당일 시간 범위 내 유저의 가장 최근 퀴즈 락 조회 (난이도 변경 검증용)
     */
    Optional<UserQuiz> findTopByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID userId, LocalDateTime from, LocalDateTime to);

    /**
     * 이번 주(월~일) solved_date 가 설정된 레코드 수 집계.
     * GreaterThanEqual(>=) + LessThanEqual(<=) 로 월요일·일요일 양쪽 경계값을 모두 포함한다.
     */
    long countByUserIdAndSolvedDateGreaterThanEqualAndSolvedDateLessThanEqual(
            UUID userId, LocalDate from, LocalDate to);

    /**
     * 특정 날짜에 solved_date 가 설정된 레코드 존재 여부 (오늘 퀴즈 완료 여부 판단용).
     */
    boolean existsByUserIdAndSolvedDate(UUID userId, LocalDate solvedDate);

    /**
     * 정답 제출 시 중복 보상 방지를 위한 비관적 잠금 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uq FROM UserQuiz uq WHERE uq.userId = :userId AND uq.quizId = :quizId")
    Optional<UserQuiz> findByUserIdAndQuizIdForUpdate(
            @Param("userId") UUID userId, @Param("quizId") UUID quizId);
}
