package com.akku.backend.domain.challenge.service;

import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.event.RewardRequestedEvent;
import com.akku.backend.domain.challenge.event.RewardTransferredEvent;
import com.akku.backend.domain.challenge.exception.ChallengeErrorCode;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpendingChallengeService {

    private final SpendingChallengeRepository spendingChallengeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountService accountService;

    /*
        1. 소비 챌린지 등록 (자녀)
     */
    @Transactional
    public SpendingChallengeDto.CreateResponse createChallenge(UUID userId, SpendingChallengeDto.CreateRequest request) {

        // 1. 유저 정보 조회 및 자녀 역할 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        LocalDate today = LocalDate.now();

        // 2. 다가오는 다음 주 월요일 및 일요일 날짜 계산
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextSunday = nextMonday.plusDays(6);

        // 3. 동일한 주간, 동일한 카테고리에 대한 중복 등록 검증
        boolean isDuplicate = spendingChallengeRepository.existsByUserAndSubCategoryNameAndStartDate(
                user, request.getCategory(), nextMonday
        );

        if (isDuplicate) {
            throw new ApiException(ChallengeErrorCode.DUPLICATE_CHALLENGE);
        }

        // 4. 소비 챌린지 엔티티 생성 및 초기 상태 설정
        SpendingChallenge challenge = SpendingChallenge.builder()
                .user(user)
                .subCategoryName(request.getCategory())
                .targetSpending(request.getTargetSpending())
                .rewardAmount(request.getRewardAmount())
                .status(ChallengeStatus.PENDING)
                .startDate(nextMonday)
                .endDate(nextSunday)
                .build();

        SpendingChallenge savedChallenge;
        try {
            savedChallenge = spendingChallengeRepository.saveAndFlush(challenge);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ChallengeErrorCode.DUPLICATE_CHALLENGE);
        }

        return SpendingChallengeDto.CreateResponse.builder()
                .challengeId(savedChallenge.getId())
                .status(savedChallenge.getStatus().name())
                .build();
    }

    /*
        2. 소비 챌린지 수정 (자녀)
        - 승인 대기, 거절 상태에서만 수정 가능
     */
    @Transactional
    public SpendingChallengeDto.UpdateResponse updateChallenge(UUID userId, UUID challengeId, SpendingChallengeDto.UpdateRequest request) {

        // 1. 유저 및 챌린지 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        SpendingChallenge challenge = spendingChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        // 2. 권한 검증
        if (!challenge.getUser().getId().equals(userId)) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 3. 상태 검증 (PENDING 또는 REJECTED 상태만 수정 가능)
        if (challenge.getStatus() != ChallengeStatus.PENDING && challenge.getStatus() != ChallengeStatus.REJECTED) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 4. 기한(Deadline) 검증
        // 오늘 날짜가 챌린지의 시작일(월요일)과 같거나 지났다면, 이미 해당 주간이 시작된 것이므로 수정 불가
        if (!LocalDate.now().isBefore(challenge.getStartDate())) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 5. 카테고리 중복 검증 (카테고리를 변경한 경우에만)
        if (!challenge.getSubCategoryName().equals(request.getCategory())) {
            boolean isDuplicate = spendingChallengeRepository.existsByUserAndSubCategoryNameAndStartDate(
                    user, request.getCategory(), challenge.getStartDate()
            );
            if (isDuplicate) {
                throw new ApiException(ChallengeErrorCode.DUPLICATE_CHALLENGE);
            }
        }

        // 6. 데이터 업데이트
        challenge.updateChallenge(request.getCategory(), request.getTargetSpending(), request.getRewardAmount());

        // 7. REJECTED 상태였다면 다시 PENDING으로 돌리고 반려 메시지 초기화
        if (challenge.getStatus() == ChallengeStatus.REJECTED) {
            challenge.updateStatus(ChallengeStatus.PENDING);
            challenge.clearParentMessage();
        }

        // 8. DB 제약 위반을 메서드 경계 안에서 잡기 위해 명시적 flush
        try {
            spendingChallengeRepository.saveAndFlush(challenge);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ChallengeErrorCode.DUPLICATE_CHALLENGE);
        }

        return SpendingChallengeDto.UpdateResponse.builder()
                .challengeId(challenge.getId())
                .status(challenge.getStatus().name())
                .build();
    }

    /*
        3. 소비 챌린지 삭제 (자녀)
        - 승인 대기, 거절 상태에서만 삭제 가능
     */
    @Transactional
    public void deleteChallenge(UUID userId, UUID challengeId) {

        // 1. 유저 및 챌린지 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        SpendingChallenge challenge = spendingChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        // 2. 권한 검증
        if (!challenge.getUser().getId().equals(userId)) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 3. 상태 검증 (PENDING 또는 REJECTED 상태만 삭제 가능)
        if (challenge.getStatus() != ChallengeStatus.PENDING && challenge.getStatus() != ChallengeStatus.REJECTED) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 4. Hard Delete
        spendingChallengeRepository.delete(challenge);
    }

    /*
        4. 소비 챌린지 승인/반려 (부모)
        - 승인 대기중인 상태에서만 가능
     */
    @Transactional
    public SpendingChallengeDto.StatusUpdateResponse updateChallengeStatus(UUID parentId, UUID challengeId, SpendingChallengeDto.StatusUpdateRequest request) {

        // 1. 부모 유저 및 챌린지 조회
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        SpendingChallenge challenge = spendingChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        // 2. 부모 역할 검증
        if (!"PARENT".equals(parent.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 3. 가족 관계 검증 (부모와 챌린지 소유 자녀가 같은 familyId)
        if (parent.getFamilyId() == null || challenge.getUser().getFamilyId() == null
                || !parent.getFamilyId().equals(challenge.getUser().getFamilyId())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 4. 요청된 상태값 유효성 검증 (APPROVED 또는 REJECTED만 허용)
        if (request.getStatus() != ChallengeStatus.APPROVED && request.getStatus() != ChallengeStatus.REJECTED) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 5. 현재 챌린지 상태 검증 (PENDING 상태에서만 승인/반려 가능)
        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 6. 상태 및 메시지 업데이트 (Dirty Checking)
        challenge.replyToChallenge(request.getStatus(), request.getParentMessage());

        return SpendingChallengeDto.StatusUpdateResponse.builder()
                .challengeId(challenge.getId())
                .status(challenge.getStatus().name())
                .parentMessage(challenge.getParentMessage())
                .build();
    }

    /*
        4-1. 이번 주 소비 목표 챌린지 목록 조회 (자녀 전용 — 종합 챌린지 Facade 전용)
        - startDate = 이번 주 월요일인 챌린지 전체 반환 (상태 무관)
        - CHILD 역할 검증만 수행; 역할 우회 방어선은 Facade/Controller 에도 있으나 여기서도 유지
     */
    public List<SpendingChallengeDto.ChallengeSummary> getThisWeekChallenges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        return spendingChallengeRepository.findAllByUserAndStartDate(user, thisMonday)
                .stream()
                .map(c -> SpendingChallengeDto.ChallengeSummary.builder()
                        .challengeId(c.getId())
                        .category(c.getSubCategoryName())
                        .targetSpending(c.getTargetSpending())
                        .rewardAmount(c.getRewardAmount())
                        .status(c.getStatus().name())
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .build())
                .collect(Collectors.toList());
    }

    /*
        5. 차주 소비 목표 챌린지 목록 조회 (부모/자녀 공통)
     */
    public SpendingChallengeDto.ListResponse getNextWeekChallenges(UUID requestUserId, UUID childId, ChallengeStatus status) {

        // 1. 요청자 조회
        User requestUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        User searchUser;

        // 2. 권한에 따른 타겟 유저 결정 및 가족 매칭 검증
        String role = requestUser.getRole(); // String 타입이므로 .name() 제거

        if ("CHILD".equals(role)) {
            // 자녀인 경우 본인의 데이터만 조회
            searchUser = requestUser;
        } else if ("PARENT".equals(role)) {
            // 부모인 경우 childId 필수
            if (childId == null) {
                throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE); // BAD_REQUEST 성격의 에러 코드로 대체 권장
            }
            // 자녀 유저 조회
            searchUser = userRepository.findById(childId)
                    .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

            // 가족 매칭 검증 (UUID 타입의 familyId 직접 비교)
            if (requestUser.getFamilyId() == null || searchUser.getFamilyId() == null ||
                    !requestUser.getFamilyId().equals(searchUser.getFamilyId())) {
                throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
            }
        } else {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 3. 차주 월요일 날짜 계산
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        // 4. 데이터 조회 (상태 필터링 유무 분기)
        List<SpendingChallenge> challenges;
        if (status != null) {
            challenges = spendingChallengeRepository.findAllByUserAndStartDateAndStatus(searchUser, nextMonday, status);
        } else {
            challenges = spendingChallengeRepository.findAllByUserAndStartDate(searchUser, nextMonday);
        }

        // 5. DTO 변환 (Collectors 임포트 필요)
        List<SpendingChallengeDto.ChallengeSummary> summaries = challenges.stream()
                .map(c -> SpendingChallengeDto.ChallengeSummary.builder()
                        .challengeId(c.getId())
                        .category(c.getSubCategoryName())
                        .targetSpending(c.getTargetSpending())
                        .rewardAmount(c.getRewardAmount())
                        .status(c.getStatus().name())
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .build())
                .collect(Collectors.toList());

        return SpendingChallengeDto.ListResponse.builder()
                .challenges(summaries)
                .build();
    }

    /*
        6. 소비 챌린지 단건 상세 조회 (부모/자녀 공통)
     */
    public SpendingChallengeDto.DetailResponse getChallengeDetail(UUID requestUserId, UUID challengeId) {

        // 1. 요청자 및 챌린지 조회 (기존 로직 동일)
        User requestUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        SpendingChallenge challenge = spendingChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        // 2. 권한 검증 (기존 로직 동일)
        String role = requestUser.getRole();
        if ("CHILD".equals(role)) {
            if (!challenge.getUser().getId().equals(requestUserId)) {
                throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
            }
        } else if ("PARENT".equals(role)) {
            if (requestUser.getFamilyId() == null || challenge.getUser().getFamilyId() == null ||
                    !requestUser.getFamilyId().equals(challenge.getUser().getFamilyId())) {
                throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
            }
        } else {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 3. 실시간 소비 누적액 계산 (월요일 00:00:00 ~ 다음 월요일 00:00:00, 반개방 구간)
        LocalDateTime startDateTime = challenge.getStartDate().atStartOfDay();
        LocalDateTime endExclusive = challenge.getEndDate().plusDays(1).atStartOfDay();

        // 변경된 부분: transactionRepository 대신 spendingChallengeRepository의 Native Query 호출
        Long currentSpending = spendingChallengeRepository.calculateCurrentSpending(
                challenge.getUser().getId(),
                challenge.getSubCategoryName(),
                startDateTime,
                endExclusive
        );

        // 4. 응답 DTO 반환
        return SpendingChallengeDto.DetailResponse.builder()
                .challengeId(challenge.getId())
                .category(challenge.getSubCategoryName())
                .targetSpending(challenge.getTargetSpending())
                .rewardAmount(challenge.getRewardAmount())
                .status(challenge.getStatus().name())
                .parentMessage(challenge.getParentMessage())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .currentSpending(currentSpending)
                .build();
    }

    /*
        7. 미수령 보상 목록 조회 (자녀 전용)
        - 상태가 SUCCESS이고, endDate가 '지난주 일요일'인 챌린지만 반환
        - 오늘이 언제든 항상 "직전 주의 일요일"을 정확히 계산
     */
    public SpendingChallengeDto.UnclaimedListResponse getUnclaimedChallenges(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        LocalDate lastSunday = resolveLastSunday();

        List<SpendingChallenge> challenges =
                spendingChallengeRepository.findAllByUserAndStatusAndEndDate(user, ChallengeStatus.SUCCESS, lastSunday);

        List<SpendingChallengeDto.ChallengeSummary> summaries = challenges.stream()
                .map(c -> SpendingChallengeDto.ChallengeSummary.builder()
                        .challengeId(c.getId())
                        .category(c.getSubCategoryName())
                        .targetSpending(c.getTargetSpending())
                        .rewardAmount(c.getRewardAmount())
                        .status(c.getStatus().name())
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .build())
                .collect(Collectors.toList());

        return SpendingChallengeDto.UnclaimedListResponse.builder()
                .challenges(summaries)
                .build();
    }

    /*
        8. 보상 요청 (자녀 전용)
        - SUCCESS 상태이며 endDate가 지난주 일요일인 챌린지에 대해서만 요청 가능
        - 상태를 REWARD_REQUESTED로 변경 후 이벤트 발행 → 부모 FCM 알림
     */
    @Transactional
    public SpendingChallengeDto.RewardRequestResponse requestReward(UUID userId, UUID challengeId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        SpendingChallenge challenge = spendingChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        // 1. 자녀 본인의 챌린지인지 검증
        if (!challenge.getUser().getId().equals(userId)) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 2. 상태 검증 — 정확히 SUCCESS 상태여야만 요청 가능
        if (challenge.getStatus() != ChallengeStatus.SUCCESS) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 3. 기한 검증 — endDate가 '지난주 일요일'이어야만 요청 가능 (2주 이상 지난 챌린지는 기한 만료)
        LocalDate lastSunday = resolveLastSunday();
        if (!challenge.getEndDate().equals(lastSunday)) {
            throw new ApiException(ChallengeErrorCode.REWARD_PERIOD_EXPIRED);
        }

        // 4. 상태 변경 (Dirty Checking)
        challenge.updateStatus(ChallengeStatus.REWARD_REQUESTED);

        // 5. 이벤트 발행 — AFTER_COMMIT 이후 리스너가 부모에게 FCM 알림 발송
        eventPublisher.publishEvent(new RewardRequestedEvent(
                challenge.getId(),
                user.getFamilyId(),
                user.getName(),
                challenge.getSubCategoryName(),
                challenge.getRewardAmount()
        ));

        return SpendingChallengeDto.RewardRequestResponse.builder()
                .challengeId(challenge.getId())
                .status(challenge.getStatus().name())
                .build();
    }

    /*
        9. 보상 송금 처리 (부모 전용)
        - REWARD_REQUESTED 상태 검증 → 이중 출금 방지 핵심 가드
        - AccountService.internalRewardTransfer() 호출 → 실패 시 예외 전파 → 전체 트랜잭션 롤백
        - 성공 시 REWARDED 상태로 변경 후 자녀 FCM 이벤트 발행
     */
    @Transactional
    public SpendingChallengeDto.RewardTransferResponse processRewardTransfer(UUID parentId, UUID challengeId,
                                                                              SpendingChallengeDto.RewardTransferRequest request) {

        // 1. 부모 유저 및 챌린지 조회
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 비관적 락으로 조회 — 동일 챌린지에 대한 동시 송금 요청이 직렬화됨
        SpendingChallenge challenge = spendingChallengeRepository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new ApiException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        User child = challenge.getUser();

        // 2. 부모 역할 검증
        if (!"PARENT".equals(parent.getRole())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 3. 가족 관계 검증 (부모와 챌린지 소유 자녀가 같은 familyId)
        if (parent.getFamilyId() == null || child.getFamilyId() == null
                || !parent.getFamilyId().equals(child.getFamilyId())) {
            throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        }

        // 4. 상태 검증 — 이중 출금 방지 핵심 가드 (REWARD_REQUESTED 상태에서만 진입 허용)
        if (challenge.getStatus() != ChallengeStatus.REWARD_REQUESTED) {
            throw new ApiException(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        // 5. 적요(메모) 구성
        String depositMemo = String.format("주간 소비 챌린지 보상 (%s)", challenge.getSubCategoryName());
        String withdrawalMemo = String.format("챌린지 보상 송금 (%s)", challenge.getSubCategoryName());

        // 6. 송금 실행 (AccountService 위임 — 잔액 부족 등 실패 시 예외 전파 → 트랜잭션 롤백)
        accountService.internalRewardTransfer(
                parentId,
                child.getId(),
                request.getParentAccountId(),
                request.getChildAccountId(),
                challenge.getRewardAmount(),
                depositMemo,
                withdrawalMemo
        );

        // 7. 챌린지 상태 REWARDED로 업데이트 (Dirty Checking)
        challenge.updateStatus(ChallengeStatus.REWARDED);

        // 8. 자녀 FCM 알림 이벤트 발행 (AFTER_COMMIT 이후 비동기 처리)
        eventPublisher.publishEvent(new RewardTransferredEvent(
                challenge.getId(),
                child.getId(),
                challenge.getRewardAmount(),
                challenge.getSubCategoryName()
        ));

        return SpendingChallengeDto.RewardTransferResponse.builder()
                .challengeId(challenge.getId())
                .status(ChallengeStatus.REWARDED.name())
                .rewardAmount(challenge.getRewardAmount())
                .build();
    }

    /**
     * "지난주 일요일" 날짜 계산.
     * 오늘이 무슨 요일이든 항상 직전 주의 일요일을 반환한다.
     *   - 이번 주 월요일 = today.with(previousOrSame(MONDAY))
     *   - 지난주 일요일 = 이번 주 월요일 - 1일
     */
    private LocalDate resolveLastSunday() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusDays(1);
    }
}