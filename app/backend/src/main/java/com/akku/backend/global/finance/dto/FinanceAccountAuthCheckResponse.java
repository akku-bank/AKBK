package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountAuthCheckResponse(
    @JsonProperty("REC") Rec rec
) {
    public record Rec(
        @JsonProperty("status") String status,
        @JsonProperty("transactionUniqueNo") Long transactionUniqueNo,
        @JsonProperty("accountNo") String accountNo
    ) {}
}
