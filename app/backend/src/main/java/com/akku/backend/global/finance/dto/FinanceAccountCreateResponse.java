package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountCreateResponse(
    @JsonProperty("REC") Rec rec
) {
    public record Rec(
        @JsonProperty("bankCode") String bankCode,
        @JsonProperty("accountNo") String accountNo,
        @JsonProperty("currency") Currency currency
    ) {}

    public record Currency(
        @JsonProperty("currency") String currency,
        @JsonProperty("currencyName") String currencyName
    ) {}
}
