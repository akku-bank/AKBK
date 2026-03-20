package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "카드 거래 내역 응답")
public record CardHistoryResponse(
    @Schema(description = "청구 예정 금액")
    Long estimatedBalance,
    
    @Schema(description = "거래 상세 내역 목록")
    List<CardTransactionDetails> list
) {
    public record CardTransactionDetails(
        @Schema(description = "거래 고유 번호")
        Long transactionUniqueNo,
        
        @Schema(description = "카테고리 ID")
        String categoryId,
        
        @Schema(description = "카테고리명")
        String categoryName,
        
        @Schema(description = "가맹점 ID")
        Long merchantId,
        
        @Schema(description = "가맹점명")
        String merchantName,

        @Schema(description = "거래 일자")
        String transactionDate,
        
        @Schema(description = "거래 시간")
        String transactionTime,
        
        @Schema(description = "거래 금액")
        Long transactionBalance
    ) {}
}
