package com.akku.backend.domain.challenge.event;

import java.util.UUID;

/**
 * 자녀의 보상 요청 이벤트.
 * SpendingChallengeService에서 발행하고, RewardRequestedEventListener에서 부모 FCM 알림으로 처리한다.
 */
public record RewardRequestedEvent(
        UUID challengeId,
        UUID familyId,
        String childName,
        String category,
        Long rewardAmount
) {}
