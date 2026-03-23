package com.akku.backend.global.finance.dto;

public record FinanceAccountAuthCheckRequest(
    String accountNo,
    String authText,
    String authCode
) {}
