package com.akku.backend.domain.quiz.repository;

import com.akku.backend.domain.quiz.entity.ChatLog;
import com.akku.backend.domain.quiz.entity.ChatLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * chat_logs 테이블 레포지토리
 */
public interface ChatLogRepository extends JpaRepository<ChatLog, ChatLogId> {

    /**
     * 유저 ID + 퀴즈 ID로 채팅 로그 조회
     */
    Optional<ChatLog> findByUserIdAndQuizId(UUID userId, UUID quizId);

    /**
     * 원자적 UPSERT — 레코드가 없으면 삽입, 있으면 chat_json만 갱신.
     * PostgreSQL ON CONFLICT를 사용하여 TOCTOU 경쟁 조건을 방지한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO chat_logs (user_id, quiz_id, chat_json, created_at)
            VALUES (:userId, :quizId, CAST(:chatJson AS jsonb), CURRENT_DATE)
            ON CONFLICT (user_id, quiz_id)
            DO UPDATE SET chat_json = CAST(EXCLUDED.chat_json AS jsonb)
            """, nativeQuery = true)
    void upsertChatJson(
            @Param("userId") UUID userId,
            @Param("quizId") UUID quizId,
            @Param("chatJson") String chatJson);
}
