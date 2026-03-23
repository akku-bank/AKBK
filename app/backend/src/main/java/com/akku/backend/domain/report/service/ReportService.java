package com.akku.backend.domain.report.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.report.dto.WeeklyReportResponse;
import com.akku.backend.domain.report.entity.WeeklyCategoryRatio;
import com.akku.backend.domain.report.entity.WeeklyReport;
import com.akku.backend.domain.report.repository.WeeklyCategoryRatioRepository;
import com.akku.backend.domain.report.repository.WeeklyReportRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final UserRepository userRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final WeeklyCategoryRatioRepository weeklyCategoryRatioRepository;

    public WeeklyReportResponse getWeeklyReport(UUID userId, LocalDate date) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        return generateWeeklyReport(user, date);
    }

    public WeeklyReportResponse getChildWeeklyReport(UUID parentId, UUID childId, LocalDate date) {
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        User child = userRepository.findById(childId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 부모와 자녀가 같은 가족인지 확인
        if (parent.getFamilyId() == null || !parent.getFamilyId().equals(child.getFamilyId())) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        return generateWeeklyReport(child, date);
    }

    private WeeklyReportResponse generateWeeklyReport(User user, LocalDate date) {
        // 주간 시작일(월요일)과 종료일(일요일) 계산
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        // DB에서 해당 주차의 리포트 내역 조회
        List<WeeklyReport> reports = weeklyReportRepository.findByIdUserIdAndIdStartDay(userId, weekStart);
        
        // 지출과 수입 레코드 분리
        WeeklyReport spendReport = reports.stream()
                .filter(r -> "SPEND".equals(r.getId().getType()))
                .findFirst()
                .orElse(null);
                
        WeeklyReport incomeReport = reports.stream()
                .filter(r -> "INCOME".equals(r.getId().getType()))
                .findFirst()
                .orElse(null);

        // 카테고리별 지출 비율 조회
        List<WeeklyCategoryRatio> categoryRatios = weeklyCategoryRatioRepository.findByIdUserIdAndIdStartDay(userId, weekStart);

        return WeeklyReportResponse.builder()
                .reportId(userId.toString() + "_" + weekStart.toString())
                .weekStartDate(weekStart.toString())
                .weekEndDate(weekEnd.toString())
                .totalSpending(spendReport != null ? spendReport.getTotalAmount() : 0L)
                .totalIncome(incomeReport != null ? incomeReport.getTotalAmount() : 0L)
                .dailySpending(mapToDailySpending(spendReport))
                .categoryRatios(categoryRatios.stream()
                        .map(ratio -> WeeklyReportResponse.CategoryRatioData.builder()
                                .subCategoryId(ratio.getId().getSubCategoryId())
                                .subCategoryName(ratio.getSubCategoryName())
                                .ratio(ratio.getRatio())
                                .spendingAmount(ratio.getSpendingAmount())
                                .build())
                        .collect(Collectors.toList()))
                .aiSpendingSummary(spendReport != null ? spendReport.getSpendingAiSummary() : "이번 주 소비 내역을 분석하고 있습니다.")
                .aiQuizSummary(spendReport != null ? spendReport.getQuizAiSummary() : "금융 퀴즈 리포트를 생성 중입니다.")
                .build();
    }

    private WeeklyReportResponse.DailySpending mapToDailySpending(WeeklyReport report) {
        if (report == null) {
            return WeeklyReportResponse.DailySpending.builder()
                    .mon(0L).tue(0L).wed(0L).thu(0L).fri(0L).sat(0L).sun(0L)
                    .build();
        }
        return WeeklyReportResponse.DailySpending.builder()
                .mon(report.getMon()).tue(report.getTue()).wed(report.getWed())
                .thu(report.getThu()).fri(report.getFri()).sat(report.getSat()).sun(report.getSun())
                .build();
    }
}
