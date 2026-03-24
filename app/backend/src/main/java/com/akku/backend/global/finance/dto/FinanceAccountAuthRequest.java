package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountAuthRequest(
    @JsonProperty("accountNo") String accountNo,
    @JsonProperty("authText") String authText
) {}
