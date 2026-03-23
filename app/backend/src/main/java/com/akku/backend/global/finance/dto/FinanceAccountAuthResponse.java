package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountAuthResponse(
    @JsonProperty("REC") Rec rec
) {
    public record Rec(
        @JsonProperty("transactionUniqueNo") Long transactionUniqueNo,
        @JsonProperty("accountNo") String accountNo
    ) {}
}
