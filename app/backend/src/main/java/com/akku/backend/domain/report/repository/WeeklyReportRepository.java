package com.akku.backend.domain.report.repository;

import com.akku.backend.domain.report.entity.WeeklyReport;
import com.akku.backend.domain.report.entity.WeeklyReportId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, WeeklyReportId> {
    List<WeeklyReport> findByIdUserIdAndIdStartDay(UUID userId, LocalDate startDay);
}
