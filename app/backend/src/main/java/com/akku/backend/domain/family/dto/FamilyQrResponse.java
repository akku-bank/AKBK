package com.akku.backend.domain.family.dto;

import java.time.LocalDateTime;

public record FamilyQrResponse(
        String qrCode,
        LocalDateTime qrExpiresAt
) {
}