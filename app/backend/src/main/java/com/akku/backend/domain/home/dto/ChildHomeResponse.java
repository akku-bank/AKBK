package com.akku.backend.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record ChildHomeResponse(
        long cashBalance,
        long jellingBalance,
        int level,
        int score,
        boolean hasLevelChanged,
        boolean hasUnreadNotification,
        int donationGauge,
        AvatarDto avatar
) {
    public record AvatarDto(
            String eyeType,
            String skinColor,
            List<EquippedItemDto> equippedItems
    ) {}

    public record EquippedItemDto(
            UUID itemId,
            String category,
            String resourceUrl
    ) {}
}