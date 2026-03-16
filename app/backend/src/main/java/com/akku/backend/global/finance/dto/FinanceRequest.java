package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceRequest<T>(
    @JsonProperty("Header") FinanceRequestHeader header,
    @JsonUnwrapped T data
) {}
