package com.akku.backend.global.finance.dto;

public record FinanceAccountAuthRequest(
    String accountNo,
    String authText
) {}
