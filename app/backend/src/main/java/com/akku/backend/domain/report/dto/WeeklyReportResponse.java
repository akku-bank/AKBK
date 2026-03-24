package com.akku.backend.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportResponse {
    private String reportId; // userId + startDate 조합 가상 ID
    private String weekStartDate;
    private String weekEndDate;
    private Long totalIncome;
    private Long totalSpending;
    
    // 요일별 지출 내역
    private DailySpending dailySpending;
    
    // 카테고리별 지출 비율
    private List<CategoryRatioData> categoryRatios;
    
    private String aiSpendingSummary;
    private String aiQuizSummary;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySpending {
        private Long mon;
        private Long tue;
        private Long wed;
        private Long thu;
        private Long fri;
        private Long sat;
        private Long sun;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRatioData {
        private Integer subCategoryId;
        private String subCategoryName;
        private BigDecimal ratio;
        private Long spendingAmount;
    }
}
