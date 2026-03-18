package com.akku.backend.domain.quiz.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * chat_logs 테이블 복합 PK 클래스 (user_id, quiz_id)
 */
public class ChatLogId implements Serializable {

    private UUID userId;
    private UUID quizId;

    public ChatLogId() {}

    public ChatLogId(UUID userId, UUID quizId) {
        this.userId = userId;
        this.quizId = quizId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatLogId that)) return false;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(quizId, that.quizId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, quizId);
    }
}
