package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCardCreateResponse(
    @JsonProperty("REC") Rec rec
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rec(
        @JsonProperty("cardNo") String cardNo,
        @JsonProperty("cvc") String cvc,
        @JsonProperty("cardUniqueNo") String cardUniqueNo,
        @JsonProperty("cardIssuerCode") String cardIssuerCode,
        @JsonProperty("cardIssuerName") String cardIssuerName,
        @JsonProperty("cardName") String cardName,
        @JsonProperty("cardExpiryDate") String cardExpiryDate,
        @JsonProperty("withdrawalAccountNo") String withdrawalAccountNo,
        @JsonProperty("withdrawalDate") String withdrawalDate
    ) {}
}
