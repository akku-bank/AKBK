package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OfflinePaymentTokenResponse {

    @Schema(description = "1회용 결제 QR 토큰", example = "ABC123XYZ789")
    private String qrToken;

    @Schema(description = "토큰 만료 시간", example = "180")
    private Integer expiresIn;

    public static OfflinePaymentTokenResponse of(String token, int expiresIn) {
        return OfflinePaymentTokenResponse.builder()
                .qrToken(token)
                .expiresIn(expiresIn)
                .build();
    }
}
