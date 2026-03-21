package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record FinanceCardProductListResponse(
    @JsonProperty("REC") List<CardProductDetails> rec
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardProductDetails(
        @JsonProperty("cardUniqueNo") String cardUniqueNo,
        @JsonProperty("cardIssuerCode") String cardIssuerCode,
        @JsonProperty("cardIssuerName") String cardIssuerName,
        @JsonProperty("cardName") String cardName,
        @JsonProperty("cardTypeCode") String cardTypeCode,
        @JsonProperty("cardTypeName") String cardTypeName,
        @JsonProperty("baseLimitPerformance") Long baseLimitPerformance,
        @JsonProperty("maxBenefitLimit") Long maxBenefitLimit,
        @JsonProperty("cardDescription") String cardDescription,
        @JsonProperty("cardBenefitInfo") List<Map<String, String>> cardBenefitInfo
    ) {}
}
