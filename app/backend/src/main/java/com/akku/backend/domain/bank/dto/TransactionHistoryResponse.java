package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "거래 내역 조회 응답")
public record TransactionHistoryResponse(
    @Schema(description = "해당 월의 거래 내역 리스트")
    List<TransactionInfo> transactions
) {
    public record TransactionInfo(
        @Schema(description = "거래 ID", example = "tx-123")
        String id,
        @Schema(description = "결제 일시", example = "20260312134353")
        String date,
        @Schema(description = "가맹점/내역명", example = "스타벅스")
        String merchantName,
        @Schema(description = "결제 금액 (+/-)", example = "-5000")
        Long amount,
        @Schema(description = "숨김 처리 여부", example = "false")
        Boolean isHidden
    ) {}
}
