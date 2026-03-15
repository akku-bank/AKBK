package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "계좌 이체 요청")
public record TransferRequest(
    @NotBlank(message = "출금 계좌 ID는 필수입니다.")
    @Schema(description = "돈을 보낼 내 계좌 ID", example = "acc-uuid-my123")
    String withdrawalAccountId,

    @NotBlank(message = "입금 계좌 ID는 필수입니다.")
    @Schema(description = "돈을 받을 상대방 계좌 ID", example = "acc-uuid-target123")
    String targetAccountId,

    @NotNull(message = "이체 금액은 필수입니다.")
    @Schema(description = "이체할 금액", example = "10000")
    Long amount,

    @NotBlank(message = "결제 비밀번호는 필수입니다.")
    @Schema(description = "승인용 6자리 간편 비밀번호", example = "123456")
    String pin
) {}
