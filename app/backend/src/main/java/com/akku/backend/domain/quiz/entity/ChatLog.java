package com.akku.backend.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * AI 채팅 로그 엔티티 (chat_logs)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@IdClass(ChatLogId.class)
@Table(name = "chat_logs")
public class ChatLog {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    /**
     * 채팅 기록 — PostgreSQL JSONB 컬럼 (전체 대화 내역 직렬화)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_json", nullable = false, columnDefinition = "jsonb")
    private String chatJson;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Builder
    public ChatLog(UUID userId, UUID quizId, String chatJson) {
        this.userId = userId;
        this.quizId = quizId;
        this.chatJson = chatJson;
        this.createdAt = LocalDate.now();
    }

    /**
     * FastAPI 응답 수신 시 채팅 로그를 최신 내용으로 갱신
     */
    public void updateChatJson(String newChatJson) {
        this.chatJson = newChatJson;
    }
}
