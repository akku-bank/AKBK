package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "계좌 이체 요청")
public record TransferRequest(
    @NotBlank(message = "출금 계좌 ID는 필수입니다.")
    @Schema(description = "돈을 보낼 내 계좌 ID", example = "acc-uuid-my123")
    String withdrawalAccountId,

    @NotBlank(message = "은행 코드는 필수입니다.")
    @Schema(description = "받는 사람 은행 코드", example = "001")
    String targetBankCode,

    @NotBlank(message = "계좌 번호는 필수입니다.")
    @Schema(description = "받는 사람 계좌 번호", example = "1234567890")
    String targetAccountNumber,

    @NotBlank(message = "받는 사람 이름은 필수입니다.")
    @Schema(description = "받는 사람 이름", example = "김싸피")
    String targetName,

    @NotNull(message = "이체 금액은 필수입니다.")
    @Positive(message = "이체 금액은 0보다 커야 합니다.")
    @Schema(description = "이체할 금액", example = "10000")
    Long amount,

    @NotBlank(message = "결제 비밀번호는 필수입니다.")
    @Schema(description = "승인용 6자리 간편 비밀번호", example = "123456")
    String pin
) {}
