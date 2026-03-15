package com.akku.backend.domain.bank.dto;

public record AccountLinkRequest(
    String bankCode,
    String accountNumber
) {}
