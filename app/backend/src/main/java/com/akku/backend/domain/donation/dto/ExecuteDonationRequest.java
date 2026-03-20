package com.akku.backend.domain.donation.dto;

import jakarta.validation.constraints.Positive;

public record ExecuteDonationRequest(
        @Positive(message = "기부 금액은 0보다 커야 합니다.")
        Integer amount
) {
}
