package com.akku.backend.domain.bank.dto;

import java.util.UUID;

public record AccountCreateResponse(
    UUID accountId,
    long balance
) {}
