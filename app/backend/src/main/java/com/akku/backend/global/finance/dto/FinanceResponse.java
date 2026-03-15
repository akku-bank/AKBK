package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceResponse<T>(
    @JsonProperty("Header") FinanceResponseHeader header,
    T data
) {}
