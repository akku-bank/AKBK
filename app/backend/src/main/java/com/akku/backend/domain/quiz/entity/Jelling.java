package com.akku.backend.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 젤링 지갑 엔티티 (jellings)
 * user_id가 PK이자 FK (users)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "jellings")
public class Jelling {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private long balance;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Jelling(UUID userId, long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    /**
     * 젤링 보상 적립 — Setter 대신 명시적 도메인 메서드 사용
     */
    public void addBalance(long amount) {
        this.balance += amount;
    }
}
