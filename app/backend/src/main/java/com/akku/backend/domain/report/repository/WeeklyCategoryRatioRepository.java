package com.akku.backend.domain.report.repository;

import com.akku.backend.domain.report.entity.WeeklyCategoryRatio;
import com.akku.backend.domain.report.entity.WeeklyCategoryRatioId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WeeklyCategoryRatioRepository extends JpaRepository<WeeklyCategoryRatio, WeeklyCategoryRatioId> {
    List<WeeklyCategoryRatio> findByIdUserIdAndIdStartDay(UUID userId, LocalDate startDay);
}
