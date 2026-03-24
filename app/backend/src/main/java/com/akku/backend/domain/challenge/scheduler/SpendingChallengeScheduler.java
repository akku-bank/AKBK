package com.akku.backend.domain.challenge.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주간 챌린지 정산 스케줄러 — 매주 월요일 00:00 실행.
 *
 * STEP 1: 지난주 IN_PROGRESS 챌린지 최종 정산 (SUCCESS / FAIL)
 * STEP 2: 이번주 APPROVED 챌린지 활성화 (IN_PROGRESS 전환)
 *
 * 각 STEP은 SpendingChallengeStepService의 독립된 @Transactional 메서드로 위임되어,
 * STEP 1 실패 시에도 STEP 2(챌린지 활성화)가 중단되지 않음.
 *
 * ShedLock 적용으로 다중 서버 환경에서 중복 실행 방지.
 * lockAtMostFor: 예상 실행 시간(3분)을 초과하면 다른 서버가 실행 가능하도록 설정.
 * lockAtLeastFor: 실행이 너무 빨리 끝나도 최소 1분간 락을 유지하여 거의 동시 실행 방지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpendingChallengeScheduler {

    private final SpendingChallengeStepService stepService;

    @Scheduled(cron = "0 0 0 * * MON")
    @SchedulerLock(name = "spendingChallenge_weeklySettle", lockAtMostFor = "3m", lockAtLeastFor = "1m")
    public void settleWeeklyChallenges() {
        log.info("주간 챌린지 정산 스케줄러 시작");

        try {
            stepService.settleLastWeek();
        } catch (Exception e) {
            log.error("STEP 1 (지난주 정산) 실패 — STEP 2는 계속 진행", e);
        }

        try {
            stepService.activateThisWeek();
        } catch (Exception e) {
            log.error("STEP 2 (이번주 활성화) 실패", e);
        }

        log.info("주간 챌린지 정산 스케줄러 완료");
    }
}
