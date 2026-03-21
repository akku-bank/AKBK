package com.akku.backend.domain.challenge.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

}