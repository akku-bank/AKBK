package com.akku.backend.domain.report.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.report.dto.WeeklyReportResponse;
import com.akku.backend.domain.report.entity.WeeklyReport;
import com.akku.backend.domain.report.entity.WeeklyReportId;
import com.akku.backend.domain.report.repository.WeeklyCategoryRatioRepository;
import com.akku.backend.domain.report.repository.WeeklyReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private WeeklyCategoryRatioRepository weeklyCategoryRatioRepository;

    @Test
    @DisplayName("주간 리포트 조회 - 데이터가 있는 경우 성공적으로 반환한다")
    void getWeeklyReport_Success() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2024, 3, 24); // 일요일
        LocalDate weekStart = LocalDate.of(2024, 3, 18); // 월요일

        User user = User.builder().id(userId).build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        WeeklyReportId spendReportId = new WeeklyReportId(userId, weekStart, "SPEND");
        WeeklyReport spendReport = WeeklyReport.builder()
                .id(spendReportId)
                .totalAmount(50000L)
                .mon(10000L).tue(5000L).wed(0L).thu(15000L).fri(20000L).sat(0L).sun(0L)
                .spendingAiSummary("소비 분석 결과")
                .quizAiSummary("퀴즈 분석 결과")
                .build();

        WeeklyReportId incomeReportId = new WeeklyReportId(userId, weekStart, "INCOME");
        WeeklyReport incomeReport = WeeklyReport.builder()
                .id(incomeReportId)
                .totalAmount(30000L)
                .mon(0L).tue(30000L).wed(0L).thu(0L).fri(0L).sat(0L).sun(0L)
                .build();

        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(userId, weekStart))
                .willReturn(List.of(spendReport, incomeReport));
        given(weeklyCategoryRatioRepository.findByIdUserIdAndIdStartDay(userId, weekStart))
                .willReturn(List.of());

        // when
        WeeklyReportResponse result = reportService.getWeeklyReport(userId, date);

        // then
        assertThat(result.getWeekStartDate()).isEqualTo(weekStart.toString());
        assertThat(result.getTotalSpending()).isEqualTo(50000L);
        assertThat(result.getDailySpending().getMon()).isEqualTo(10000L);
        assertThat(result.getTotalIncome()).isEqualTo(30000L);
        assertThat(result.getDailyIncome().getTue()).isEqualTo(30000L);
        assertThat(result.getDailyIncome().getMon()).isEqualTo(0L);
        assertThat(result.getAiSpendingSummary()).isEqualTo("소비 분석 결과");
        assertThat(result.getReportId()).isNotNull();
    }

    @Test
    @DisplayName("주간 리포트 조회 - 데이터가 없는 경우 기본값을 반환한다")
    void getWeeklyReport_Empty() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2024, 3, 24);
        LocalDate weekStart = LocalDate.of(2024, 3, 18);

        User user = User.builder().id(userId).build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(userId, weekStart)).willReturn(List.of());
        given(weeklyCategoryRatioRepository.findByIdUserIdAndIdStartDay(userId, weekStart)).willReturn(List.of());

        // when
        WeeklyReportResponse result = reportService.getWeeklyReport(userId, date);

        // then
        assertThat(result.getTotalSpending()).isEqualTo(0L);
        assertThat(result.getTotalIncome()).isEqualTo(0L);
        assertThat(result.getDailySpending().getMon()).isEqualTo(0L);
        assertThat(result.getDailyIncome().getMon()).isEqualTo(0L);
        assertThat(result.getAiSpendingSummary()).isEqualTo("이번 주 소비 내역을 분석하고 있습니다.");
    }
}
