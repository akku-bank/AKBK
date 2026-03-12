package com.akku.backend.domain.avatar.dto;

import java.util.UUID;

public record AvatarItemResponse(
        UUID itemId,
        String category,
        String name,
        String resourceUrl,
        int requiredLevel,
        boolean isOwned,
        boolean isLevelLocked
) {
}


