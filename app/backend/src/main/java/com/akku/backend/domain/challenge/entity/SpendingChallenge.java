package com.akku.backend.domain.challenge.entity;

import com.akku.backend.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "spending_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpendingChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 자녀 정보

    @Column(name = "sub_category_name", length = 50, nullable = false)
    private String subCategoryName;

    @Column(name = "target_spending", nullable = false)
    private Long targetSpending;

    @Column(name = "reward_amount", nullable = false)
    private Long rewardAmount;

    @Column(name = "parent_message", columnDefinition = "TEXT")
    private String parentMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ChallengeStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 상태 변경을 위한 편의 메서드
    public void updateChallenge(String subCategoryName, Long targetSpending, Long rewardAmount) {
        this.subCategoryName = subCategoryName;
        this.targetSpending = targetSpending;
        this.rewardAmount = rewardAmount;
    }

    // 상태 변경
    public void updateStatus(ChallengeStatus status) {
        this.status = status;
    }

    // 반려 사유 초기화
    public void clearParentMessage() {
        this.parentMessage = null;
    }

    // 부모의 승인/반려 처리
    public void replyToChallenge(ChallengeStatus status, String parentMessage) {
        this.status = status;
        this.parentMessage = parentMessage;
    }

}