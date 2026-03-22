package com.akku.backend.domain.challenge.scheduler;

import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주간 챌린지 정산 스케줄러 — 매주 월요일 00:00 실행.
 *
 * STEP 1: 지난주 IN_PROGRESS 챌린지 최종 정산 (SUCCESS / FAIL)
 * STEP 2: 이번주 APPROVED 챌린지 활성화 (IN_PROGRESS 전환)
 *
 * 각 STEP은 독립된 @Transactional 메서드로 분리되어,
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

    private final SpendingChallengeRepository spendingChallengeRepository;

    @Scheduled(cron = "0 0 0 * * MON")
    @SchedulerLock(name = "spendingChallenge_weeklySettle", lockAtMostFor = "3m", lockAtLeastFor = "1m")
    public void settleWeeklyChallenges() {
        log.info("주간 챌린지 정산 스케줄러 시작");

        try {
            settleLastWeek();
        } catch (Exception e) {
            log.error("STEP 1 (지난주 정산) 실패 — STEP 2는 계속 진행", e);
        }

        try {
            activateThisWeek();
        } catch (Exception e) {
            log.error("STEP 2 (이번주 활성화) 실패", e);
        }

        log.info("주간 챌린지 정산 스케줄러 완료");
    }

    /**
     * STEP 1: 지난주 IN_PROGRESS 챌린지 → SUCCESS / FAIL 최종 판정.
     *
     * 스케줄러는 월요일 00:00에 실행되므로, 어제(일요일)가 지난주 endDate.
     * FAIL 판정: 이벤트 기반 실시간 FAIL이 이미 처리했을 수 있으나,
     *   실시간 이벤트 누락(서버 재시작 등)에 대한 방어 로직으로 유지.
     * 멱등성 보장: IN_PROGRESS 챌린지만 조회하므로 중복 실행 시 이미 처리된 챌린지는 건드리지 않음.
     */
    @Transactional
    public void settleLastWeek() {
        LocalDate lastSunday = LocalDate.now().minusDays(1); // 월요일 기준 어제 = 지난주 일요일
        LocalDate lastMonday = lastSunday.minusDays(6);      // 지난주 월요일

        List<SpendingChallenge> challenges =
                spendingChallengeRepository.findAllByStatusAndEndDate(ChallengeStatus.IN_PROGRESS, lastSunday);

        log.info("STEP 1: 지난주({} ~ {}) 정산 대상 챌린지 수: {}", lastMonday, lastSunday, challenges.size());

        for (SpendingChallenge challenge : challenges) {
            LocalDateTime startDateTime = lastMonday.atStartOfDay();
            LocalDateTime endDateTime = lastSunday.atTime(23, 59, 59);

            Long currentSpending = spendingChallengeRepository.calculateCurrentSpending(
                    challenge.getUser().getId(),
                    challenge.getSubCategoryName(),
                    startDateTime,
                    endDateTime
            );

            if (currentSpending <= challenge.getTargetSpending()) {
                challenge.updateStatus(ChallengeStatus.SUCCESS);
                log.info("SUCCESS 판정 — challengeId: {}, userId: {}, spending: {}/{}",
                        challenge.getId(), challenge.getUser().getId(), currentSpending, challenge.getTargetSpending());
            } else {
                challenge.updateStatus(ChallengeStatus.FAIL);
                log.info("FAIL 판정 (방어 로직) — challengeId: {}, userId: {}, spending: {}/{}",
                        challenge.getId(), challenge.getUser().getId(), currentSpending, challenge.getTargetSpending());
            }
        }
    }

    /**
     * STEP 2: 이번주 APPROVED 챌린지 → IN_PROGRESS 활성화.
     *
     * 스케줄러 실행 시점이 이번주 월요일 00:00이므로, startDate = 오늘(thisMonday)인 챌린지 활성화.
     */
    @Transactional
    public void activateThisWeek() {
        LocalDate thisMonday = LocalDate.now(); // 월요일 00:00에 실행

        List<SpendingChallenge> challenges =
                spendingChallengeRepository.findAllByStatusAndStartDate(ChallengeStatus.APPROVED, thisMonday);

        log.info("STEP 2: 이번주({}) 활성화 대상 챌린지 수: {}", thisMonday, challenges.size());

        for (SpendingChallenge challenge : challenges) {
            challenge.updateStatus(ChallengeStatus.IN_PROGRESS);
            log.info("IN_PROGRESS 전환 — challengeId: {}, userId: {}",
                    challenge.getId(), challenge.getUser().getId());
        }
    }
}
