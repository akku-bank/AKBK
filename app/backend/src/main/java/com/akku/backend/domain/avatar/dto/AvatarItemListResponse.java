package com.akku.backend.domain.avatar.dto;

import java.util.List;

public record AvatarItemListResponse(
        List<AvatarItemResponse> items
) {
}
