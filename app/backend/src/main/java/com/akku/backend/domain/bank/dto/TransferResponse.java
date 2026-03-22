package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계좌 이체 응답")
public record TransferResponse(
    @Schema(description = "이체 거래 ID", example = "tx-uuid-123456")
    String transactionId,

    @Schema(description = "이체 후 내 계좌 잔액", example = "90000")
    Long remainBalance
) {}
