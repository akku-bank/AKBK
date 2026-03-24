package com.akku.backend.domain.bank.dto;

import java.util.UUID;

public record AccountLinkResponse(
    UUID accountId,
    String bankName
) {}
