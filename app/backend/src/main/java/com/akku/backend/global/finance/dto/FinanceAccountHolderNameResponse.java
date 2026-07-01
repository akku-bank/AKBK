package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceAccountHolderNameResponse(
    @JsonProperty("accountNo") String accountNo,
    @JsonProperty("userName") String userName
) {}
