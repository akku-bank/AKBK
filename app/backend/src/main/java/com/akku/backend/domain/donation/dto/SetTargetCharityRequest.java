package com.akku.backend.domain.donation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetTargetCharityRequest(
        @NotNull(message = "기부처 ID는 필수입니다.")
        UUID charityId
) {
}
