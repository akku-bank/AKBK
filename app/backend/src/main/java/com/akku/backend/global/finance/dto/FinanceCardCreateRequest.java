package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCardCreateRequest(
    @JsonProperty("cardUniqueNo") String cardUniqueNo,
    @JsonProperty("withdrawalAccountNo") String withdrawalAccountNo,
    @JsonProperty("withdrawalDate") String withdrawalDate
) {}
