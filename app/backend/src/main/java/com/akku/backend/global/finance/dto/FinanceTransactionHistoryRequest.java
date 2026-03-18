package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceTransactionHistoryRequest(
    @JsonProperty("accountNo") String accountNo,
    @JsonProperty("startDate") String startDate,
    @JsonProperty("endDate") String endDate,
    @JsonProperty("transactionType") String transactionType, // M: 입금, D: 출금, A: 전체
    @JsonProperty("orderByType") String orderByType // DESC, ASC
) {}
