package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FinanceTransactionHistoryResponse(
    @JsonProperty("REC") Rec rec
) {
    public record Rec(
        @JsonProperty("totalCount") String totalCount,
        @JsonProperty("list") List<TransactionDetails> list
    ) {}

    public record TransactionDetails(
        @JsonProperty("transactionUniqueNo") String transactionUniqueNo,
        @JsonProperty("transactionDate") String transactionDate,
        @JsonProperty("transactionTime") String transactionTime,
        @JsonProperty("transactionType") String transactionType,
        @JsonProperty("transactionTypeName") String transactionTypeName,
        @JsonProperty("transactionAccountNo") String transactionAccountNo,
        @JsonProperty("transactionBalance") String transactionBalance,
        @JsonProperty("transactionAfterBalance") String transactionAfterBalance,
        @JsonProperty("transactionSummary") String transactionSummary,
        @JsonProperty("transactionMemo") String transactionMemo
    ) {}
}
