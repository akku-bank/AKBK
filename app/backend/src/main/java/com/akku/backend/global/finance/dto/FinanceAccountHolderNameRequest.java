package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountHolderNameRequest(
    @JsonProperty("accountNo") String accountNo
) {}
