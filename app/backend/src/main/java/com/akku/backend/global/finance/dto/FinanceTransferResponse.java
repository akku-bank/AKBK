package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FinanceTransferResponse(
    @JsonProperty("REC") List<Rec> rec
) {
    public record Rec(
        @JsonProperty("transactionUniqueNo") String transactionUniqueNo,
        @JsonProperty("transactionDate") String transactionDate,
        @JsonProperty("transactionType") String transactionType,
        @JsonProperty("transactionTypeName") String transactionTypeName,
        @JsonProperty("transactionAccountNo") String transactionAccountNo
    ) {}
}
