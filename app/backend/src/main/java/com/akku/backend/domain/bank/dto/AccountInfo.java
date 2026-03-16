package com.akku.backend.domain.bank.dto;

public record AccountInfo(
    String bankCode,
    String bankName,
    String accountNumber,
    String accountName,
    long balance
) {}
