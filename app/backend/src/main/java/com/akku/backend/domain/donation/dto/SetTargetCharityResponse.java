package com.akku.backend.domain.donation.dto;

import java.util.UUID;

public record SetTargetCharityResponse(
        UUID activeCharityId
) {
}
