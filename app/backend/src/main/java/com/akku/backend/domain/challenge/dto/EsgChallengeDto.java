package com.akku.backend.domain.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public class EsgChallengeDto {

    /**
     * GET /api/challenges/esg 응답
     * DB status → isCompleted / isRewarded boolean 매핑
     *   isCompleted = status != IN_PROGRESS
     *   isRewarded  = status == REWARDED
     */
    @Getter
    @Builder
    public static class StatusResponse {
        private UUID challengeId;
        @JsonProperty("isCompleted")
        private boolean isCompleted;
        @JsonProperty("isRewarded")
        private boolean isRewarded;
        private Long rewardAmount;
    }

    /**
     * POST /api/challenges/esg/{challengeId}/rewards 응답
     * 보상 수령 완료 후 최종 젤링 잔액 포함
     */
    @Getter
    @Builder
    public static class RewardResponse {
        private UUID challengeId;
        private Long remainJelling;
    }
}
