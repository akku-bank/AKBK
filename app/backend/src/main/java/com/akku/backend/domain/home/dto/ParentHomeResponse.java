package com.akku.backend.domain.home.dto;

import java.util.List;
import java.util.UUID;

public record ParentHomeResponse(
        long parentBalance,
        int pendingChallengeCount,
        List<ChildSummaryDto> children
) {
    public record ChildSummaryDto(
            UUID childId,
            String name,
            long balance,
            long weeklySpending
    ) {}
}