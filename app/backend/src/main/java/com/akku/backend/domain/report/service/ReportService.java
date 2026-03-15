package com.akku.backend.domain.report.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.report.dto.WeeklyReportResponse;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final UserRepository userRepository;
    private final SsafyFinanceService ssafyFinanceService;

    public WeeklyReportResponse getWeeklyReport(UUID userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 주간 시작일(월요일)과 종료일(일요일) 계산
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        String startStr = weekStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endStr = weekEnd.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 금융망에서 카드 목록 조회
        var cards = ssafyFinanceService.getCards(user.getUserKey());

        // 각 카드별 해당 기간의 거래 내역 조회 및 지출 합계 계산
        long totalSpending = 0L;
        for (var card : cards) {
            var transactions = ssafyFinanceService.getCardTransactions(
                    user.getUserKey(), card.getCardNo(), startStr, endStr);
            
            for (var t : transactions) {
                if ("1".equals(t.getTransactionType())) { // 출금/결제
                    totalSpending += t.getTransactionAmount();
                }
            }
        }

        // AI 요약 (추후 AI 연동)
        String aiSpendingSummary = "이번 주 소비 내역을 분석하고 있습니다. 곧 멋진 분석 결과를 보여드릴게요!";
        String aiQuizSummary = "금융 퀴즈를 통해 매일매일 똑똑해지고 있어요! 다음 주 리포트도 기대해주세요.";

        return WeeklyReportResponse.builder()
                .reportId(UUID.randomUUID())
                .weekStartDate(weekStart.toString())
                .weekEndDate(weekEnd.toString())
                .totalSpending(totalSpending)
                .aiSpendingSummary(aiSpendingSummary)
                .aiQuizSummary(aiQuizSummary)
                .build();
    }
}
