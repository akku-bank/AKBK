package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "거래 내역 조회 응답")
public record TransactionHistoryResponse(
    @Schema(description = "계좌 현재 잔액 (동기화됨)", example = "50000")
    Long balance,
    @Schema(description = "해당 월의 거래 내역 리스트")
    List<TransactionInfo> transactions
) {
    public record TransactionInfo(
        @Schema(description = "거래 ID", example = "tx-123")
        String id,
        @Schema(description = "결제 일시", example = "20260312134353")
        String date,
        @Schema(description = "가맹점/내역명", example = "스타벅스")
        String place,
        @Schema(description = "결제 금액", example = "5000")
        Long amount,
        @Schema(description = "입금 여부", example = "true")
        Boolean isIncome,
        @Schema(description = "거래 후 잔액", example = "45000")
        Long balanceAfter,
        @Schema(description = "숨김 처리 여부", example = "false")
        Boolean isHidden,
        @Schema(description = "메모", example = "점심 식사")
        String memo
    ) {}
}
