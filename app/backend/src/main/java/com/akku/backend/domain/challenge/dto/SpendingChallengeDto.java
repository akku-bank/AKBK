package com.akku.backend.domain.challenge.dto;

import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SpendingChallengeDto {

    // --- 등록 (Create) ---
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String category;         // 카테고리 (sub_category_name에 매핑)
        private Long targetSpending;     // 목표 소비 금액
        private Long rewardAmount;       // 요청할 보상(용돈) 금액
    }

    @Getter
    @Builder
    public static class CreateResponse {
        private UUID challengeId;
        private String status;
    }

    // --- 수정 (Update) ---
    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String category;
        private Long targetSpending;
        private Long rewardAmount;
    }

    @Getter
    @Builder
    public static class UpdateResponse {
        private UUID challengeId;
        private String status;
    }

    // --- 상태 변경 (Status Update - 부모 전용) ---
    @Getter
    @NoArgsConstructor
    public static class StatusUpdateRequest {
        private ChallengeStatus status; // APPROVED 또는 REJECTED
        private String parentMessage;   // 승인 응원 메시지 또는 반려 사유
    }

    @Getter
    @Builder
    public static class StatusUpdateResponse {
        private UUID challengeId;
        private String status;
        private String parentMessage;
    }

    // --- 다건 조회 (List - 차주 챌린지) ---
    @Getter
    @Builder
    public static class ChallengeSummary {
        private UUID challengeId;
        private String category;         // DB의 subCategoryName 매핑
        private Long targetSpending;
        private Long rewardAmount;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter
    @Builder
    public static class ListResponse {
        private List<ChallengeSummary> challenges;
    }

    // --- 단건 상세 조회 (Detail) ---
    @Getter
    @Builder
    public static class DetailResponse {
        private UUID challengeId;
        private String category;
        private Long targetSpending;
        private Long rewardAmount;
        private String status;
        private String parentMessage; // 반려 사유 또는 응원 메시지
        private LocalDate startDate;
        private LocalDate endDate;
        private Long currentSpending; // 실시간 누적 소비액
    }

    // --- 미수령 보상 목록 조회 (Unclaimed - 자녀 전용) ---
    // ChallengeSummary를 재사용하고, 목록 래퍼만 별도 정의
    @Getter
    @Builder
    public static class UnclaimedListResponse {
        private List<ChallengeSummary> challenges;
    }

    // --- 보상 요청 응답 (Reward Request) ---
    @Getter
    @Builder
    public static class RewardRequestResponse {
        private UUID challengeId;
        private String status; // REWARD_REQUESTED
    }

    // --- 보상 송금 응답 (Reward Transfer - 부모 전용) ---
    @Getter
    @Builder
    public static class RewardTransferResponse {
        private UUID challengeId;
        private String status; // REWARDED
        private Long rewardAmount;
    }
}