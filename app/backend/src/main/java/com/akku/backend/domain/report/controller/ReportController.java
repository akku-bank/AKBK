package com.akku.backend.domain.report.controller;

import com.akku.backend.domain.report.dto.WeeklyReportResponse;
import com.akku.backend.domain.report.service.ReportService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Report", description = "리포트 관련 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final Clock clock;

    @Operation(summary = "내 주간 리포트 조회", description = "로그인한 사용자의 주간 소비 및 활동 리포트를 조회합니다.")
    @GetMapping("/me/weekly")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> getMyWeeklyReport(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now(clock);
        }
        WeeklyReportResponse response = reportService.getWeeklyReport(userId, date);
        return ResponseEntity.ok(ApiResponse.success("주간 리포트 조회 성공", response));
    }

    @Operation(summary = "자녀 주간 리포트 조회", description = "부모가 특정 자녀의 주간 소비 및 활동 리포트를 조회합니다.")
    @GetMapping("/children/{childId}/weekly")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> getChildWeeklyReport(
            @AuthenticationPrincipal UUID parentId,
            @PathVariable UUID childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now(clock);
        }
        WeeklyReportResponse response = reportService.getChildWeeklyReport(parentId, childId, date);
        return ResponseEntity.ok(ApiResponse.success("자녀 주간 리포트 조회 성공", response));
    }
}
