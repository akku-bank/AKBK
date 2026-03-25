package com.akku.backend.domain.report.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 주간 소비 레벨 정산 배치를 주기적으로 실행하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyLevelScheduler {

    private final JobLauncher jobLauncher;
    private final Job weeklyLevelUpdateJob;

    /**
     * 매주 월요일 00:00:00에 지난주(월-일) 데이터를 바탕으로 레벨 정산 시작
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void runWeeklyLevelUpdateJob() {
        log.info("[Scheduler] Starting Weekly Level Update Job at {}", LocalDateTime.now());
        
        try {
            // 실행 시각을 파라미터로 넣어 동일한 Job의 중복 방지 및 이력 관리
            JobParameters params = new JobParametersBuilder()
                    .addString("datetime", LocalDateTime.now().toString())
                    .toJobParameters();
            
            jobLauncher.run(weeklyLevelUpdateJob, params);
            
            log.info("[Scheduler] Weekly Level Update Job launched successfully.");
        } catch (Exception e) {
            log.error("[Scheduler] Error occurred while launching Weekly Level Update Job", e);
        }
    }
}
