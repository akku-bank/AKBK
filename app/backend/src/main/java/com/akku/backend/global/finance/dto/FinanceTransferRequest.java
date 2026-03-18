package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceTransferRequest(
    @JsonProperty("depositBankCode") String depositBankCode,
    @JsonProperty("depositAccountNo") String depositAccountNo,
    @JsonProperty("transactionBalance") Long transactionBalance,
    @JsonProperty("withdrawalBankCode") String withdrawalBankCode,
    @JsonProperty("withdrawalAccountNo") String withdrawalAccountNo,
    @JsonProperty("depositTransactionMemo") String depositTransactionMemo,
    @JsonProperty("withdrawalTransactionMemo") String withdrawalTransactionMemo
) {}
