package com.akku.backend.domain.avatar.dto;

import java.util.List;
import java.util.UUID;

public record AvatarEquipmentUpdateRequest(
        List<UUID> equippedItemIds
) {
}