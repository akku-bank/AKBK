package com.akku.backend.domain.donation.dto;

import java.util.UUID;

public record SetTargetCharityRequest(
        UUID charityId
) {
}
