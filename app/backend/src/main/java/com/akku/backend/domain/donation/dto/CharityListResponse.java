package com.akku.backend.domain.donation.dto;

import java.util.List;

public record CharityListResponse(
        List<CharityResponse> charities
) {
}
