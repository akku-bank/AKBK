package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FinanceCardTransactionHistoryResponse(
    @JsonProperty("REC") Rec rec
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rec(
        @JsonProperty("cardIssuerCode") String cardIssuerCode,
        @JsonProperty("cardIssuerName") String cardIssuerName,
        @JsonProperty("cardName") String cardName,
        @JsonProperty("cardNo") String cardNo,
        @JsonProperty("estimatedBalance") Long estimatedBalance,
        @JsonProperty("transactionList") List<TransactionDetails> list
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionDetails(
        @JsonProperty("transactionUniqueNo") Long transactionUniqueNo,
        @JsonProperty("categoryId") String categoryId,
        @JsonProperty("categoryName") String categoryName,
        @JsonProperty("merchantId") Long merchantId,
        @JsonProperty("merchantName") String merchantName,
        @JsonProperty("transactionDate") String transactionDate,
        @JsonProperty("transactionTime") String transactionTime,
        @JsonProperty("transactionBalance") Long transactionBalance,
        @JsonProperty("cardStatus") String cardStatus,
        @JsonProperty("billStatementsYn") String billStatementsYn,
        @JsonProperty("billStatementsStatus") String billStatementsStatus
    ) {}
}
