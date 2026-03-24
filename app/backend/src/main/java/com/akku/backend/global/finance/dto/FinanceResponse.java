package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record FinanceResponse<T>(
    @JsonProperty("Header") FinanceResponseHeader header,
    @JsonUnwrapped T data
) {}
