package com.akku.backend.domain.donation.dto;

public record JellingHubResponse(
        Long remainJelling,
        ActiveCharityInfo activeCharity
) {
}
