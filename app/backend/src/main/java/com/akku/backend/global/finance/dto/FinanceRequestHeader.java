package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record FinanceRequestHeader(
    @JsonProperty("apiName") String apiName,
    @JsonProperty("transmissionDate") String transmissionDate,
    @JsonProperty("transmissionTime") String transmissionTime,
    @JsonProperty("institutionCode") String institutionCode,
    @JsonProperty("fintechAppNo") String fintechAppNo,
    @JsonProperty("apiServiceCode") String apiServiceCode,
    @JsonProperty("institutionTransactionUniqueNo") String institutionTransactionUniqueNo,
    @JsonProperty("apiKey") String apiKey,
    @JsonProperty("userKey") String userKey
) {}
