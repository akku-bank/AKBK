package com.akku.backend.domain.donation.dto;

public record ExecuteDonationResponse(
        Long remainJelling,
        Long currentAmount,
        Boolean isCompleted
) {
}
