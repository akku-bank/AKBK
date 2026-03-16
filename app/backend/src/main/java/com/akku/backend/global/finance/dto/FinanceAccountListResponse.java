package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FinanceAccountListResponse(
    @JsonProperty("REC") List<AccountDetails> rec
) {
    public record AccountDetails(
        @JsonProperty("bankCode") String bankCode,
        @JsonProperty("bankName") String bankName,
        @JsonProperty("userName") String userName,
        @JsonProperty("accountNo") String accountNo,
        @JsonProperty("accountName") String accountName,
        @JsonProperty("accountTypeCode") String accountTypeCode,
        @JsonProperty("accountTypeName") String accountTypeName,
        @JsonProperty("accountCreatedDate") String accountCreatedDate,
        @JsonProperty("accountExpiryDate") String accountExpiryDate,
        @JsonProperty("dailyTransferLimit") Long dailyTransferLimit,
        @JsonProperty("oneTimeTransferLimit") Long oneTimeTransferLimit,
        @JsonProperty("accountBalance") Long accountBalance,
        @JsonProperty("lastTransactionDate") String lastTransactionDate,
        @JsonProperty("currency") String currency
    ) {}
}
