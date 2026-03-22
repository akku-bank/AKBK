package com.akku.backend.domain.donation.dto;

import java.util.UUID;

public record ActiveCharityInfo(
        UUID id,
        String name,
        Long currentAmount,
        Integer targetAmount
) {
}
