package com.akku.backend.global.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCardPaymentRequest(
    @JsonProperty("cardNo") String cardNo,
    @JsonProperty("cvc") String cvc,
    @JsonProperty("merchantId") Long merchantId,
    @JsonProperty("paymentBalance") Long paymentBalance
) {}
