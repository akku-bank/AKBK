package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceResponseHeader(
    @JsonProperty("responseCode") String responseCode,
    @JsonProperty("responseMessage") String responseMessage,
    @JsonProperty("apiName") String apiName,
    @JsonProperty("transmissionDate") String transmissionDate,
    @JsonProperty("transmissionTime") String transmissionTime,
    @JsonProperty("institutionCode") String institutionCode,
    @JsonProperty("fintechAppNo") String fintechAppNo,
    @JsonProperty("apiServiceCode") String apiServiceCode,
    @JsonProperty("institutionTransactionUniqueNo") String institutionTransactionUniqueNo
) {}
