package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCardTransactionHistoryRequest(
    @JsonProperty("cardNo") String cardNo,
    @JsonProperty("cvc") String cvc,
    @JsonProperty("startDate") String startDate,
    @JsonProperty("endDate") String endDate
) {}
