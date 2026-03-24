package com.akku.backend.domain.challenge.event;

import java.util.UUID;

/**
 * 부모의 보상 송금 완료 이벤트.
 * SpendingChallengeService에서 발행하고, RewardTransferredEventListener에서 자녀 FCM 알림으로 처리한다.
 */
public record RewardTransferredEvent(
        UUID challengeId,
        UUID childId,
        Long rewardAmount,
        String category
) {}
