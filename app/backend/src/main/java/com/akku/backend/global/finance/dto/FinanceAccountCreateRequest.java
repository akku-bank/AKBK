package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountCreateRequest(
    @JsonProperty("accountTypeUniqueNo") String accountTypeUniqueNo
) {}
