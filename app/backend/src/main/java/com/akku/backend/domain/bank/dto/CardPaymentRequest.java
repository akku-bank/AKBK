package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

@Schema(description = "카드 결제 요청")
public record CardPaymentRequest(
    @Schema(description = "카드 ID")
    @NotNull UUID cardId,
    
    @Schema(description = "가맹점 ID")
    @NotNull Long merchantId,
    
    @Schema(description = "거래 금액")
    @NotNull @Positive Long paymentBalance
) {}
