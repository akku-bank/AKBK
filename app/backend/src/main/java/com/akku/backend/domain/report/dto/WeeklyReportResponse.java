package com.akku.backend.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportResponse {
    private UUID reportId;
    private String weekStartDate;
    private String weekEndDate;
    private Long totalSpending;
    private String aiSpendingSummary;
    private String aiQuizSummary;
}
