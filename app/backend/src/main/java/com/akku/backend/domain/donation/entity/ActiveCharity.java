package com.akku.backend.domain.donation.entity;

import com.akku.backend.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "active_charities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ActiveCharity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charity_id", nullable = false)
    private Charity charity;

    @Column(name = "current_amount")
    @Builder.Default
    private Long currentAmount = 0L;

    @Column(length = 20)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void donate(long amount) {
        this.currentAmount += amount;
        if (this.currentAmount >= charity.getTargetAmount()) {
            this.status = "COMPLETED";
        }
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    public void markRewarded() {
        if (!isCompleted()) {
            throw new IllegalStateException("Only completed charities can be rewarded.");
        }
        this.status = "REWARDED";
    }
}
