package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "오프라인 결제 승인 요청 DTO")
public class PaymentApprovalRequest {

    @NotBlank(message = "결제 토큰은 필수입니다.")
    @Schema(description = "자녀가 제시한 1회용 결제 토큰", example = "7D2F8A1B9C3E")
    private String qrToken;

    @NotNull(message = "결제 금액은 필수입니다.")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다.")
    @Schema(description = "결제 요청 금액", example = "3000")
    private Long amount;

    @NotBlank(message = "가맹점 명은 필수입니다.")
    @Schema(description = "가맹점 이름", example = "CU 대전점")
    private String merchantName;

    @Schema(description = "업종 카테고리", example = "CONVENIENCE_STORE")
    private String category;
}
