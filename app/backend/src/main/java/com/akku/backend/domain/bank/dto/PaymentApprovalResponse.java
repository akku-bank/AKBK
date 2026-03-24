package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "오프라인 결제 승인 응답 DTO")
public class PaymentApprovalResponse {

    @Schema(description = "승인된 거래 고유 ID")
    private UUID transactionId;

    @Schema(description = "최종 승인 금액", example = "3000")
    private Long approvedAmount;

    public static PaymentApprovalResponse of(UUID transactionId, Long approvedAmount) {
        return PaymentApprovalResponse.builder()
                .transactionId(transactionId)
                .approvedAmount(approvedAmount)
                .build();
    }
}
