package com.akku.backend.domain.donation.dto;

import java.util.UUID;

public record CharityResponse(
        UUID charityId,
        String name,
        String description
) {
}
