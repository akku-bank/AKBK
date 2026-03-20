package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCardPaymentResponse(
    @JsonProperty("REC") Rec rec
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rec(
        @JsonProperty("transactionUniqueNo") Long transactionUniqueNo,
        @JsonProperty("categoryId") String categoryId,
        @JsonProperty("categoryName") String categoryName,
        @JsonProperty("merchantId") Long merchantId,
        @JsonProperty("merchantName") String merchantName,
        @JsonProperty("transactionDate") String transactionDate,
        @JsonProperty("transactionTime") String transactionTime,
        @JsonProperty("paymentBalance") Long paymentBalance
    ) {}
}
