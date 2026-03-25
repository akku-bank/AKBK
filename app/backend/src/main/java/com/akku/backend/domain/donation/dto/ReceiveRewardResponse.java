package com.akku.backend.domain.donation.dto;

import java.util.UUID;

public record ReceiveRewardResponse(
        UUID itemId,
        String category,
        String rewardItemName,
        String resourceUrl,
        Boolean isDuplicate
) {
}
