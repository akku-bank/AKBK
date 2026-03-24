package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountAuthCheckRequest(
    @JsonProperty("accountNo") String accountNo,
    @JsonProperty("authText") String authText,
    @JsonProperty("authCode") String authCode
) {}
