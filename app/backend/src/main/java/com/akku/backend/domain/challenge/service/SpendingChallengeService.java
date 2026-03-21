package com.akku.backend.domain.challenge.service;

import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import com.akku.backend.domain.challenge.exception.ChallengeErrorCode;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.global.error.ApiException;
// UserErrorCode 등 유저 도메인 에러 코드가 있다고 가정 (import 생략)
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

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
                // TODO: UserErrorCode.USER_NOT_FOUND 등 유저 도메인 에러 코드로 대체 필요
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

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
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

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
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

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
}