package com.akku.backend.domain.report.entity;

import com.akku.backend.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "weekly_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WeeklyReport {

    @EmbeddedId
    private WeeklyReportId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "end_day")
    private LocalDate endDay;

    @Column(nullable = false)
    private Long mon;

    @Column(nullable = false)
    private Long tue;

    @Column(nullable = false)
    private Long wed;

    @Column(nullable = false)
    private Long thu;

    @Column(nullable = false)
    private Long fri;

    @Column(nullable = false)
    private Long sat;

    @Column(nullable = false)
    private Long sun;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "quiz_ai_summary", columnDefinition = "TEXT")
    private String quizAiSummary;

    @Column(name = "spending_ai_summary", columnDefinition = "TEXT")
    private String spendingAiSummary;

    @Column(name = "start_balance")
    private Long startBalance;

    public void updateStartBalance(Long startBalance) {
        this.startBalance = startBalance;
    }
}
