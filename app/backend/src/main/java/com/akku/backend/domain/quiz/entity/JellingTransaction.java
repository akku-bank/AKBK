package com.akku.backend.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 젤링 변동 내역 엔티티 (jelling_transactions)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "jelling_transactions")
public class JellingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private long amount;

    /**
     * 거래 유형 — ex) EARNED, DEDUCTED
     */
    @Column(nullable = false, length = 30)
    private String type;

    /**
     * 적립/차감 사유 — ex) 퀴즈 정답 적립
     */
    @Column(length = 100)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public JellingTransaction(UUID userId, long amount, String type, String description) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }
}
