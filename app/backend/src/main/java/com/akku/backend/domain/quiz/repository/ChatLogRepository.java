package com.akku.backend.domain.quiz.repository;

import com.akku.backend.domain.quiz.entity.ChatLog;
import com.akku.backend.domain.quiz.entity.ChatLogId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * chat_logs 테이블 레포지토리
 */
public interface ChatLogRepository extends JpaRepository<ChatLog, ChatLogId> {

    /**
     * 유저 ID + 퀴즈 ID로 채팅 로그 조회 (UPSERT 용)
     */
    Optional<ChatLog> findByUserIdAndQuizId(UUID userId, UUID quizId);
}
