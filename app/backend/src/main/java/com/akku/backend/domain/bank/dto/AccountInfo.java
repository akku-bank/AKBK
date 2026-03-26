package com.akku.backend.domain.bank.dto;

import java.util.UUID;

public record AccountInfo(
    UUID accountId,
    String bankCode,
    String bankName,
    String accountNumber,
    String accountName,
    long balance,
    boolean isPrimary
) {}
