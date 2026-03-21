package com.akku.backend.domain.donation.dto;

public record ReceiveRewardResponse(
        String rewardItemName,
        Integer customTicketCount,
        Boolean isDuplicate
) {
}
