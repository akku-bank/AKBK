package com.akku.backend.domain.challenge.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.challenge.dto.EsgChallengeDto;
import com.akku.backend.domain.challenge.entity.EsgChallenge;
import com.akku.backend.domain.challenge.entity.EsgChallengeStatus;
import com.akku.backend.domain.challenge.exception.ChallengeErrorCode;
import com.akku.backend.domain.challenge.repository.EsgChallengeRepository;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.entity.JellingTransaction;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.domain.jelling.repository.JellingTransactionRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
public class EsgChallengeService {

    private final EsgChallengeRepository esgChallengeRepository;
    private final UserRepository userRepository;
    private final JellingRepository jellingRepository;
    private final JellingTransactionRepository jellingTransactionRepository;

    /**
     * 요구사항 1 — GET /api/challenges/esg
     * 이번 주 ESG 챌린지 상태 조회 (Lazy Insert)
     *
     * 이번 주 챌린지가 없으면 IN_PROGRESS 상태로 신규 생성 후 반환.
     * 따닥 요청 시 uq_esg_user_week 제약 위반 → DataIntegrityViolationException catch 후 재조회.
     */
    @Transactional
    public EsgChallengeDto.StatusResponse getThisWeekChallenge(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisSunday = thisMonday.plusDays(6);

        EsgChallenge challenge = esgChallengeRepository
                .findByUserIdAndStartDate(userId, thisMonday)
                .orElseGet(() -> insertNewChallenge(user, thisMonday, thisSunday));

        return toStatusResponse(challenge);
    }

    /**
     * 요구사항 3 — POST /api/challenges/esg/{challengeId}/rewards
     * ESG 챌린지 보상 수령
     *
     * 비관적 락으로 이중 수령 방지.
     * 젤링 적립 실패 시 상태 업데이트도 함께 롤백 (단일 트랜잭션).
     */
    @Transactional
    public EsgChallengeDto.RewardResponse receiveReward(UUID userId, UUID challengeId) {
        // 1. 비관적 락으로 챌린지 조회 — 동시 수령 요청 직렬화
        EsgChallenge challenge = esgChallengeRepository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        // 2. 소유권 검증
        if (!challenge.getUser().getId().equals(userId)) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 3. 상태 검증 — 이중 수령 방지 핵심 가드 (SUCCESS 상태에서만 진입 허용)
        if (challenge.getStatus() != EsgChallengeStatus.SUCCESS) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 4. 젤링 적립 (비관적 락으로 잔액 정합성 보장)
        long remainJelling = addJelling(userId, challenge.getRewardAmount(), challenge.getId());

        // 5. 챌린지 상태 REWARDED 업데이트 (Dirty Checking)
        challenge.markRewarded();

        return EsgChallengeDto.RewardResponse.builder()
                .challengeId(challenge.getId())
                .remainJelling(remainJelling)
                .build();
    }

    // ─────────────────────────────────────────────────
    // private 헬퍼 메서드
    // ─────────────────────────────────────────────────

    /**
     * Lazy Insert 핵심 로직.
     * saveAndFlush()로 즉시 flush하여 unique 제약 위반을 즉시 감지.
     * DataIntegrityViolationException 발생 시 다른 스레드가 먼저 삽입한 것이므로 재조회 후 반환.
     */
    private EsgChallenge insertNewChallenge(User user, LocalDate startDate, LocalDate endDate) {
        EsgChallenge newChallenge = EsgChallenge.builder()
                .user(user)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        try {
            return esgChallengeRepository.saveAndFlush(newChallenge);
        } catch (DataIntegrityViolationException e) {
            log.debug("ESG 챌린지 동시 생성 감지 — userId: {}, startDate: {}. 기존 레코드 재조회.", user.getId(), startDate);
            return esgChallengeRepository.findByUserIdAndStartDate(user.getId(), startDate)
                    .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));
        }
    }

    /**
     * 젤링 적립 + 변동 내역 기록.
     * DonationService와 동일한 패턴으로 JellingRepository를 직접 사용.
     *
     * @return 적립 후 최종 잔액
     */
    private long addJelling(UUID userId, Long amount, UUID challengeId) {
        Jelling jelling = jellingRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.JELLING_NOT_FOUND));

        jelling.addBalance(amount);

        JellingTransaction transaction = JellingTransaction.builder()
                .user(jelling.getUser())
                .amount(amount)
                .type("ESG_REWARD")
                .description("ESG 챌린지 보상 (" + challengeId + ")")
                .build();
        jellingTransactionRepository.save(transaction);

        return jelling.getBalance();
    }

    private EsgChallengeDto.StatusResponse toStatusResponse(EsgChallenge challenge) {
        EsgChallengeStatus status = challenge.getStatus();
        return EsgChallengeDto.StatusResponse.builder()
                .challengeId(challenge.getId())
                .isCompleted(status != EsgChallengeStatus.IN_PROGRESS)
                .isRewarded(status == EsgChallengeStatus.REWARDED)
                .build();
    }
}
