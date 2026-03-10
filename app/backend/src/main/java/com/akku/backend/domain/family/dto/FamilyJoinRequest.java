package com.akku.backend.domain.family.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 가족 그룹 합류 (QR 스캔) 요청 DTO
 */
public record FamilyJoinRequest(
        @NotBlank(message = "스캔한 QR 코드는 필수입니다.")
        String scannedQrCode
) {
}