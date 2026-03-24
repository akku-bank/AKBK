package com.akku.backend.domain.report.controller;

import com.akku.backend.domain.report.dto.WeeklyReportResponse;
import com.akku.backend.domain.report.service.ReportService;
import com.akku.backend.global.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    private ReportController reportController;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 기본적으로는 시스템 시계(서울)를 사용하도록 설정 (필요 시 개별 테스트에서 재정의)
        Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));
        reportController = new ReportController(reportService, clock);
    }

    @Test
    @DisplayName("날짜 미지정 시 오늘(KST) 기준으로 리포트를 조회한다 - 월요일 새벽 1시 상황")
    void getMyWeeklyReport_DefaultDate_KST_Monday_Morning() {
        // given: 한국 시간 월요일 새벽 1시 (UTC로는 일요일 오후 4시)
        // 이 상황에서 LocalDate.now()는 UTC 기준 일요일을 반환하지만, 
        // 주입된 Clock(KST)은 월요일을 반환해야 함.
        Instant mondayMorningKst = Instant.parse("2024-03-25T01:00:00Z"); // UTC 기준 (실제 KST는 10시가 되지만 개념 확인용)
        // 실제 KST 01:00를 테스트하려면 UTC 16:00 (전날)을 사용해야 함.
        Instant monday01AMKst = Instant.parse("2024-03-24T16:00:00Z"); 
        
        Clock fixedClock = Clock.fixed(monday01AMKst, ZoneId.of("Asia/Seoul"));
        reportController = new ReportController(reportService, fixedClock);

        WeeklyReportResponse mockResponse = WeeklyReportResponse.builder()
                .reportId("test-report")
                .build();
        
        given(reportService.getWeeklyReport(eq(userId), any(LocalDate.class)))
                .willReturn(mockResponse);

        // when: 날짜 파라미터 없이 호출
        ResponseEntity<ApiResponse<WeeklyReportResponse>> response = reportController.getMyWeeklyReport(userId, null);

        // then: 
        // 1. 서비스에 전달된 날짜가 한국 시간 기준 '월요일(2024-03-25)'이어야 함.
        verify(reportService).getWeeklyReport(userId, LocalDate.of(2024, 3, 25));
        
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("주간 리포트 조회 성공");
    }

    @Test
    @DisplayName("날짜 미지정 시 오늘(KST) 기준으로 리포트를 조회한다 - 일요일 밤 23시 59분 상황")
    void getMyWeeklyReport_DefaultDate_KST_Sunday_Night() {
        // given: 한국 시간 일요일 23시 59분 59초로 시계 고정
        Instant sundayNightKst = Instant.parse("2024-03-24T14:59:59Z"); // UTC 14:59:59 = KST 23:59:59
        Clock fixedClock = Clock.fixed(sundayNightKst, ZoneId.of("Asia/Seoul"));
        reportController = new ReportController(reportService, fixedClock);

        given(reportService.getWeeklyReport(any(), any())).willReturn(WeeklyReportResponse.builder().build());

        // when
        reportController.getMyWeeklyReport(userId, null);

        // then: 여전히 월요일로 넘어가지 않고 '일요일(24일)'이어야 함.
        verify(reportService).getWeeklyReport(userId, LocalDate.of(2024, 3, 24));
    }

    @Test
    @DisplayName("날짜 미지정 시 오늘(KST) 기준으로 리포트를 조회한다 - 연말(12월 31일) 상황")
    void getMyWeeklyReport_DefaultDate_KST_YearEnd() {
        // given: 2024년 12월 31일 KST로 시계 고정
        Instant yearEndKst = Instant.parse("2024-12-30T15:00:00Z"); // KST 2024-12-31 00:00:00
        Clock fixedClock = Clock.fixed(yearEndKst, ZoneId.of("Asia/Seoul"));
        reportController = new ReportController(reportService, fixedClock);

        given(reportService.getWeeklyReport(any(), any())).willReturn(WeeklyReportResponse.builder().build());

        // when
        reportController.getMyWeeklyReport(userId, null);

        // then: 정확히 2024년 12월 31일이 전달되어야 함.
        verify(reportService).getWeeklyReport(userId, LocalDate.of(2024, 12, 31));
    }

    @Test
    @DisplayName("날짜 파라미터가 명시된 경우 해당 날짜로 리포트를 조회한다")
    void getMyWeeklyReport_WithSpecificDate() {
        // given
        LocalDate specificDate = LocalDate.of(2024, 3, 20);
        WeeklyReportResponse mockResponse = WeeklyReportResponse.builder().build();
        given(reportService.getWeeklyReport(eq(userId), eq(specificDate))).willReturn(mockResponse);

        // when
        reportController.getMyWeeklyReport(userId, specificDate);

        // then
        verify(reportService).getWeeklyReport(userId, specificDate);
    }
    
    @Test
    @DisplayName("날짜 미지정 시 자녀 리포트도 오늘(KST) 기준으로 조회한다")
    void getChildWeeklyReport_DefaultDate_KST() {
        // given: 한국 시간 월요일 새벽 1시로 시계 고정
        Instant monday01AMKst = Instant.parse("2024-03-24T16:00:00Z"); 
        Clock fixedClock = Clock.fixed(monday01AMKst, ZoneId.of("Asia/Seoul"));
        reportController = new ReportController(reportService, fixedClock);

        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        given(reportService.getChildWeeklyReport(any(), any(), any())).willReturn(WeeklyReportResponse.builder().build());

        // when: 날짜 파라미터 없이 호출
        reportController.getChildWeeklyReport(parentId, childId, null);

        // then: 주입된 Clock에 시각에 따라 '2024-03-25'가 전달되어야 함.
        verify(reportService).getChildWeeklyReport(parentId, childId, LocalDate.of(2024, 3, 25));
    }
}
