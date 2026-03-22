package com.akku.backend.domain.challenge.service;

import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import com.akku.backend.domain.challenge.exception.ChallengeErrorCode;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    /*
        1. 소비 챌린지 등록 (자녀)
     */
    @Transactional
    public SpendingChallengeDto.CreateResponse createChallenge(UUID userId, SpendingChallengeDto.CreateRequest request) {

        // 1. 유저 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

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

        SpendingChallenge savedChallenge = spendingChallengeRepository.save(challenge);

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

        // 2. 부모 권한 검증 (User 엔티티의 Role 필드 확인 - 실제 필드명에 맞게 수정 필요)
        // if (parent.getRole() != Role.PARENT) {
        //     throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        // }

        // 3. 가족 관계 검증 (부모의 가족 ID와 챌린지 등록 자녀의 가족 ID 일치 여부 확인)
        // User 엔티티에 Family 연관관계가 있다고 가정
        // if (!parent.getFamily().getId().equals(challenge.getUser().getFamily().getId())) {
        //     throw new ApiException(ChallengeErrorCode.ACCESS_DENIED);
        // }

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
}