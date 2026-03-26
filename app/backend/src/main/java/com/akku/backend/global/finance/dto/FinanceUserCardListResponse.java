package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FinanceUserCardListResponse(
    @JsonProperty("REC") List<UserCardDetails> rec
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserCardDetails(
        @JsonProperty("cardNo") String cardNo,
        @JsonProperty("cvc") String cvc,
        @JsonProperty("cardUniqueNo") String cardUniqueNo,
        @JsonProperty("cardIssuerCode") String cardIssuerCode,
        @JsonProperty("cardIssuerName") String cardIssuerName,
        @JsonProperty("cardName") String cardName,
        @JsonProperty("baselinePerformance") Long baseLimitPerformance,
        @JsonProperty("maxBenefitLimit") Long maxBenefitLimit,
        @JsonProperty("cardDescription") String cardDescription,
        @JsonProperty("cardExpiryDate") String cardExpiryDate,
        @JsonProperty("withdrawalAccountNo") String withdrawalAccountNo,
        @JsonProperty("withdrawalDate") String withdrawalDate,
        @JsonProperty("cardBenefitsInfo") List<java.util.Map<String, String>> cardBenefitInfo
    ) {}
}
